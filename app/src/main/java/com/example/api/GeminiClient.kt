package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.VaultItem
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
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
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    // Configure client with 60s timeouts as requested by guidelines
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile
    var customApiKey: String? = null

    private fun getApiKey(): String {
        return if (!customApiKey.isNullOrBlank()) customApiKey!! else BuildConfig.GEMINI_API_KEY
    }

    fun isApiKeyConfigured(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("API_KEY")
    }

    suspend fun analyzeDeadline(title: String, content: String): DeadlineAnalysis? = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            Log.w(TAG, "Gemini API key is not configured. Skipping deadline detection.")
            return@withContext null
        }

        val prompt = """
            Analyze the following shared post or message to determine if it expresses a task deadline, due date, submission date, urgent assignment, or actionable scheduled event with a time limit.
            
            Title of the shared item: "$title"
            Content/Text: "$content"
            
            Determine:
            1. "hasDeadline": Is there a genuine date/time restriction or scheduling deadline? (true or false)
            2. "deadlineText": A very concise description of the deadline, e.g. "Draft proposal due Tuesday 5:00 PM" or "Submit lab report by Friday". (Maximum 8 words. Return empty string if none).
            
            You MUST return your answer in this exact JSON format:
            {
              "hasDeadline": true,
              "deadlineText": "Draft proposal due Tuesday 5:00 PM"
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(prompt) ?: return@withContext null
            val json = JSONObject(responseText)
            val hasDeadline = json.optBoolean("hasDeadline", false)
            val deadlineText = json.optString("deadlineText", "")
            DeadlineAnalysis(hasDeadline, deadlineText)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing deadline with Gemini", e)
            null
        }
    }

    suspend fun findNecessityMatches(userQuery: String, items: List<VaultItem>): SearchAnalysis? = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext null
        }
        if (items.isEmpty()) {
            return@withContext SearchAnalysis(emptyList(), "Your vault is empty! Save some Instagram Reels, YouTube Shorts, or WhatsApp messages first.")
        }

        // Build a minimal representation of items for the LLM context
        val itemsArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.contentType)
                put("title", item.title)
                put("content", item.contentOrUrl)
                put("notes", item.notes)
                put("tags", item.tags)
            }
            itemsArray.put(obj)
        }

        val prompt = """
            The user is searching through their archived content vault (containing Reels, Shorts, and Messages) for a specific interest or necessity.
            
            User's Necessity / Query: "$userQuery"
            
            Here is the JSON list of items saved in their vault:
            ${itemsArray.toString(2)}
            
            Task:
            1. Identify up to 5 items that are most relevant to the user's query or necessity. Match semantically (e.g., if they ask for "cooking", match items with tags or descriptions of food/recipes, if they ask for "kotlin or coding", match developer items).
            2. For each relevant item, return its "id" (as an integer) and a very short 1-sentence "reason" describing exactly why it matches their necessity.
            3. Write a warm, friendly "aiResponse" in conversational language explaining how the selected vault items can address their necessity. (Do not mention database IDs in the summary).
            
            You MUST return your answer in this exact JSON format:
            {
              "results": [
                {
                  "id": 1,
                  "reason": "This tutorial shows 10 handy shortcuts that speed up your daily coding flow."
                }
              ],
              "aiResponse": "I found a few helpful items in your vault related to developer productivity, including a set of highly rating development shortcuts!"
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(prompt) ?: return@withContext null
            val json = JSONObject(responseText)
            
            val resultsArray = json.optJSONArray("results") ?: JSONArray()
            val resultsList = mutableListOf<SearchResult>()
            for (i in 0 until resultsArray.length()) {
                val itemObj = resultsArray.getJSONObject(i)
                val id = itemObj.optInt("id", -1)
                val reason = itemObj.optString("reason", "")
                if (id != -1) {
                    resultsList.add(SearchResult(id, reason))
                }
            }
            val aiResponse = json.optString("aiResponse", "Here are the most relevant vault items matching your request.")
            SearchAnalysis(resultsList, aiResponse)
        } catch (e: Exception) {
            Log.e(TAG, "Error matching necessity", e)
            null
        }
    }

    suspend fun suggestTags(title: String, content: String, notes: String): List<String>? = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext null
        }

        val prompt = """
            You are a smart cataloging system. Suggest a list of MINIMUM 10 highly relevant, single-word organic search tags/keywords for the following saved content:
            
            Title: "$title"
            Content Body / link: "$content"
            User Context Notes: "$notes"
            
            Generate at least 10 specific, neat, single-word, lowercase keywords that describe the topics, technical terms, concepts, frameworks, or categories related directly to this content.
            
            CRITICAL RULES:
            1. The tags must be highly relevant and specific to the actual content (e.g., if it is about Android UI, suggest tags like android, compose, ui, jetpack, kotlin, design).
            2. Do NOT suggest generic cataloging filler words like: saved, vault, content, link, post, video, reel, short, chat, whatsapp, message, bookmarks, details, item, smart.
            3. Do not include duplicate tags.
            4. Each tag must be a single word (no spaces, no hyphens, no special characters).
            
            You MUST return your answer in this exact JSON format:
            {
              "tags": ["tag1", "tag2", "tag3", "tag4", "tag5", "tag6", "tag7", "tag8", "tag9", "tag10", "tag11"]
            }
        """.trimIndent()

        try {
            val responseText = executeGeminiPrompt(prompt) ?: return@withContext null
            val json = JSONObject(responseText)
            val jsonArray = json.optJSONArray("tags") ?: return@withContext null
            val tagsList = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val tag = jsonArray.optString(i, "").trim().lowercase()
                    .replace(Regex("[^a-z0-9]"), "") // clean it to be neat, single word
                if (tag.isNotEmpty()) {
                    tagsList.add(tag)
                }
            }
            tagsList
        } catch (e: Exception) {
            Log.e(TAG, "Error generating tags with Gemini", e)
            null
        }
    }

    private suspend fun executeGeminiPrompt(prompt: String): String? {
        val apiKey = getApiKey()
        val url = "$BASE_URL?key=$apiKey"

        val partsArray = JSONArray().apply {
            put(JSONObject().apply { put("text", prompt) })
        }
        val contentObj = JSONObject().apply {
            put("parts", partsArray)
        }
        val contentsArray = JSONArray().apply {
            put(contentObj)
        }

        // Force JSON output structure for accuracy
        val responseFormatObj = JSONObject().apply {
            put("type", "OBJECT")
        }
        val configObj = JSONObject().apply {
            put("responseMimeType", "application/json")
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            put("generationConfig", configObj)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestBodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP error response during Gemini API execution: ${response.code} ${response.message}")
                    null
                } else {
                    val bodyString = response.body?.string() ?: ""
                    parseTextFromGeminiResponse(bodyString)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception invoking Gemini API", e)
            null
        }
    }

    private fun parseTextFromGeminiResponse(rawBody: String): String? {
        return try {
            val responseJson = JSONObject(rawBody)
            val candidates = responseJson.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val firstPart = parts.optJSONObject(0) ?: return null
            firstPart.optString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse text from Gemini response JSON", e)
            null
        }
    }
}
