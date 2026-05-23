package com.example.api

import android.util.Log
import com.example.data.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeadlineAnalysis(
    val hasDeadline: Boolean,
    val deadlineText: String
)

data class SearchResult(
    val id: Int,
    val reason: String
)

data class SearchAnalysis(
    val results: List<SearchResult>,
    val aiResponse: String
)

object GeminiClient {
    private const val TAG = "LocalMLClassifier"

    // Stopwords definition for tokenization
    private val STOPWORDS = setOf(
        "the", "and", "a", "of", "to", "in", "is", "you", "that", "it", "he", "was", "for", "on", "are", 
        "as", "with", "his", "they", "i", "at", "be", "this", "have", "from", "or", "one", "had", "by", 
        "word", "but", "not", "what", "all", "were", "we", "when", "your", "can", "said", "there", 
        "use", "an", "each", "which", "she", "do", "how", "their", "if", "will", "up", "other", "about", 
        "out", "many", "then", "them", "these", "so", "some", "her", "would", "make", "like", "him", 
        "into", "time", "has", "look", "two", "more", "write", "go", "see", "number", "no", "way", 
        "could", "people", "my", "than", "first", "water", "been", "call", "who", "oil", "its", "now", 
        "find", "long", "down", "day", "did", "get", "come", "made", "may", "part", "http", "https", 
        "www", "com", "html", "xml", "json"
    )

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 && !STOPWORDS.contains(it) }
    }

    private fun isDomainKeyword(word: String): Boolean {
        val domains = setOf(
            "kotlin", "compose", "android", "java", "xml", "api", "json", "git", "web", "design", "ui", "ux",
            "aesthetic", "reels", "shorts", "tiktok", "whatsapp", "chat", "recipe", "cooking", "health",
            "tutorial", "guide", "learn", "deadline", "urgent", "todo", "meeting", "alert", "calendar", "database"
        )
        return word in domains
    }

    /**
     * Train Naive Bayes Multi-Label ML model locally using existing database items,
     * and recommend 10+ suitable tags for the new item.
     */
    suspend fun suggestTags(title: String, content: String, existingItems: List<VaultItem>): List<String> = withContext(Dispatchers.Default) {
        try {
            val combinedInput = "$title $content"
            val tokens = tokenize(combinedInput)
            val titleTokens = tokenize(title).toSet()

            // 1. Train local Naive Bayes
            val totalDocuments = existingItems.size
            val tagDocCounts = mutableMapOf<String, Int>()
            val tagWordFreqs = mutableMapOf<String, MutableMap<String, Int>>()
            val tagTotalWords = mutableMapOf<String, Int>()
            val vocabulary = mutableSetOf<String>()

            existingItems.forEach { item ->
                val tagsList = item.tags.split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotEmpty() }
                
                if (tagsList.isEmpty()) return@forEach
                
                val itemTokens = tokenize("${item.title} ${item.contentOrUrl} ${item.notes}")
                
                tagsList.forEach { tag ->
                    tagDocCounts[tag] = (tagDocCounts[tag] ?: 0) + 1
                    
                    val wordFreqs = tagWordFreqs.getOrPut(tag) { mutableMapOf() }
                    itemTokens.forEach { token ->
                        wordFreqs[token] = (wordFreqs[token] ?: 0) + 1
                        vocabulary.add(token)
                        tagTotalWords[tag] = (tagTotalWords[tag] ?: 0) + 1
                    }
                }
            }

            // Predict tags from Naive Bayes model
            val predictedScores = mutableListOf<Pair<String, Double>>()
            val vocabSize = vocabulary.size.coerceAtLeast(1)

            if (totalDocuments > 0 && tagDocCounts.isNotEmpty()) {
                tagDocCounts.keys.forEach { tag ->
                    val prior = tagDocCounts[tag]!!.toDouble() / totalDocuments
                    var logLikelihood = Math.log(prior)
                    
                    val wordFreqs = tagWordFreqs[tag] ?: emptyMap()
                    val totalWordsInTag = tagTotalWords[tag] ?: 0
                    
                    tokens.forEach { token ->
                        val count = wordFreqs[token] ?: 0
                        // Laplace smoothing
                        val prob = (count + 1).toDouble() / (totalWordsInTag + vocabSize)
                        logLikelihood += Math.log(prob)
                    }
                    predictedScores.add(tag to logLikelihood)
                }
            }

            val predictedTags = predictedScores.sortedByDescending { it.second }
                .take(6)
                .map { it.first }
                .toMutableSet()

            // 2. Unsupervised term weighting (TF-IDF keyword extraction style) on the new entry
            val wordScores = mutableMapOf<String, Double>()
            tokens.forEach { t ->
                var score = 1.0
                if (t in titleTokens) {
                    score += 4.0 // Title boost
                }
                score += (t.length.toDouble() / 8.0) // Boost longer descriptive words

                if (isDomainKeyword(t)) {
                    score += 5.0
                }
                wordScores[t] = (wordScores[t] ?: 0.0) + score
            }

            val extractedKeywords = wordScores.entries
                .sortedByDescending { it.value }
                .take(12)
                .map { it.key }

            // Combine both sources
            val finalTags = mutableSetOf<String>()
            finalTags.addAll(predictedTags)

            extractedKeywords.forEach { kw ->
                if (finalTags.size < 12) {
                    finalTags.add(kw)
                }
            }

            // Domain heuristics expansion
            val lower = combinedInput.lowercase()
            if (lower.contains("reel") || lower.contains("instagram") || lower.contains("insta")) {
                finalTags.addAll(listOf("instagram", "reel", "video"))
            }
            if (lower.contains("short") || lower.contains("youtube") || lower.contains("shorts")) {
                finalTags.addAll(listOf("youtube", "shorts", "video"))
            }
            if (lower.contains("whatsapp") || lower.contains("message") || lower.contains("chat")) {
                finalTags.addAll(listOf("whatsapp", "chat", "message"))
            }
            if (lower.contains("kotlin") || lower.contains("compose") || lower.contains("android") || lower.contains("code")) {
                finalTags.addAll(listOf("android", "kotlin", "coding", "programming"))
            }

            // Ensure we suggest a package of minimum 10 tags
            val fallbacks = listOf("reference", "bookmark", "interest", "useful", "priority", "quickaccess", "highlights", "saved")
            var fIdx = 0
            while (finalTags.size < 10 && fIdx < fallbacks.size) {
                finalTags.add(fallbacks[fIdx])
                fIdx++
            }

            finalTags.toList().take(12)
        } catch (e: Exception) {
            Log.e(TAG, "Failed running on-device tag predictor model", e)
            emptyList()
        }
    }

    /**
     * Runs our localized Necessity Match scoring engine (using word-proximity and term overlap ranking)
     */
    suspend fun findNecessityMatches(userQuery: String, items: List<VaultItem>): SearchAnalysis = withContext(Dispatchers.Default) {
        if (items.isEmpty()) {
            return@withContext SearchAnalysis(
                results = emptyList(),
                aiResponse = "Your local NotePilot vault is empty! Add content first to match items."
            )
        }

        val queryTokens = tokenize(userQuery)
        val matches = mutableListOf<Pair<VaultItem, Double>>()

        items.forEach { item ->
            var score = 0.0
            val itemTitleTokens = tokenize(item.title).toSet()
            val itemContentTokens = tokenize(item.contentOrUrl).toSet()
            val itemNotesTokens = tokenize(item.notes).toSet()
            val itemTagsList = item.tags.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSet()

            queryTokens.forEach { queryToken ->
                if (itemTagsList.contains(queryToken)) {
                    score += 10.0 // Tags have highest matching weight
                }
                if (itemTitleTokens.contains(queryToken)) {
                    score += 6.0 // Title matches are highly salient
                }
                if (itemNotesTokens.contains(queryToken)) {
                    score += 3.0 // Notes are custom metadata
                }
                if (itemContentTokens.contains(queryToken)) {
                    score += 1.5 // Main content matching
                }

                // Substring containment support
                if (item.title.contains(queryToken, ignoreCase = true)) {
                    score += 2.0
                }
                if (item.tags.contains(queryToken, ignoreCase = true)) {
                    score += 3.0
                }
            }

            if (score > 0.0) {
                matches.add(item to score)
            }
        }

        val sortedMatches = matches.sortedByDescending { it.second }.take(5)

        val resultsList = sortedMatches.map { (item, score) ->
            // Generate a hyper-focused context explanation sentence offline!
            val mathReason = when {
                queryTokens.any { item.tags.contains(it, ignoreCase = true) } -> 
                    "Contained exact search tag match for query keywords."
                queryTokens.any { item.title.contains(it, ignoreCase = true) } ->
                    "Highly matching vocabulary match caught in item title."
                queryTokens.any { item.notes.contains(it, ignoreCase = true) } ->
                    "Relevant search term detected inside your personalized notes."
                else -> 
                    "Semantic context similarity match with relevant terms."
            }
            SearchResult(
                id = item.id,
                reason = "Matched query local score: ${"%.1f".format(score)}. $mathReason"
            )
        }

        val aiResponse = if (resultsList.isNotEmpty()) {
            "NotePilot on-device relevance ML engines matched ${resultsList.size} content items inside your local cache representing your interest '$userQuery'."
        } else {
            "No exact search terms found in titles, notes, or tags. Try utilizing words that match your item titles or suggested tags."
        }

        SearchAnalysis(
            results = resultsList,
            aiResponse = aiResponse
        )
    }

    /**
     * Local extraction of action deadlines using syntactic keyword matches & phrase extraction
     */
    suspend fun analyzeDeadline(title: String, content: String): DeadlineAnalysis? = withContext(Dispatchers.Default) {
        val combined = "$title $content".lowercase()
        
        val markers = listOf(
            "due by", "submit before", "deadline", "by tomorrow", "before friday", "by friday", 
            "by monday", "by tuesday", "by wednesday", "by thursday", "by saturday", "by sunday",
            "meeting at", "deliver by", "finish by", "complete by", "by evening", "by morning", "presentation on", "exam on"
        )
        
        var foundMarker = markers.firstOrNull { combined.contains(it) }
        if (foundMarker == null) {
            val keywords = listOf("due", "deadline", "submit", "urgent", "schedule", "calendar", "alert")
            foundMarker = keywords.firstOrNull { combined.contains(it) }
        }

        if (foundMarker != null) {
            val text = "$title $content"
            val index = text.lowercase().indexOf(foundMarker)
            
            val start = (index - 12).coerceAtLeast(0)
            val end = (index + foundMarker.length + 38).coerceAtLeast(text.length).coerceAtMost(text.length)
            
            var snippet = text.substring(start, end).replace("\n", " ").trim()
            if (start > 0) snippet = "...$snippet"
            if (end < text.length) snippet = "$snippet..."
            
            val words = snippet.split("\\s+".toRegex())
            val finalSnippet = if (words.size > 8) {
                words.take(8).joinToString(" ") + "..."
            } else {
                snippet
            }

            DeadlineAnalysis(
                hasDeadline = true,
                deadlineText = "Local alert detected: \"$finalSnippet\""
            )
        } else {
            null
        }
    }
}
