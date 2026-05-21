package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class VaultRepository(private val vaultDao: VaultDao) {
    val allItems: Flow<List<VaultItem>> = vaultDao.getAllItems()

    suspend fun insertItem(item: VaultItem): Long {
        return vaultDao.insertItem(item)
    }

    suspend fun updateItem(item: VaultItem) {
        vaultDao.updateItem(item)
    }

    suspend fun deleteItemById(id: Int) {
        vaultDao.deleteItemById(id)
    }

    suspend fun getItemById(id: Int): VaultItem? {
        return vaultDao.getItemById(id)
    }

    suspend fun prePopulateIfNeeded() {
        val currentItems = vaultDao.getAllItems().firstOrNull()
        if (currentItems.isNullOrEmpty()) {
            // WhatsApp Message - Potential Deadline Alert (Matches card 1 of the image)
            vaultDao.insertItem(
                VaultItem(
                    contentType = "WHATSAPP",
                    title = "Is this a deadline? \"Submit report by 5 PM tomorrow\"",
                    contentOrUrl = "Submit report by 5 PM tomorrow",
                    notes = "WhatsApp conversation clipping detecting an upcoming project deliverable.",
                    tags = "work, report, priority",
                    timestamp = System.currentTimeMillis() - 120000, // 2m ago
                    hasDeadline = true,
                    deadlineText = "Submit report by 5 PM tomorrow",
                    deadlineConfirmStatus = "PENDING",
                    isHighlighted = false
                )
            )

            // LinkedIn View Card (Matches card 2 of the image)
            vaultDao.insertItem(
                VaultItem(
                    contentType = "OTHER",
                    title = "Alex Chen and 3 others viewed your profile.",
                    contentOrUrl = "https://linkedin.com/in/alexchen",
                    notes = "LinkedIn activity digest showing profile impressions.",
                    tags = "linkedin, network, career",
                    timestamp = System.currentTimeMillis() - 45 * 60000, // 45m ago
                    hasDeadline = false
                )
            )

            // X Design Trends Card (Matches card 3 of the image)
            vaultDao.insertItem(
                VaultItem(
                    contentType = "REEL", // Use Reel / Video content type for card highlights
                    title = "Your post \"The Future of Minimalist Glassmorphism\" is trending.",
                    contentOrUrl = "https://x.com/trends/glassmorphism",
                    notes = "X Design Trends wave analysis and trending feedback.",
                    tags = "x, trends, design, glassmorphism",
                    timestamp = System.currentTimeMillis() - 3600000, // 1h ago
                    hasDeadline = false
                )
            )

            // Gmail Subscriptions Card (Matches card 4 of the image)
            vaultDao.insertItem(
                VaultItem(
                    contentType = "SHORT", // Map under study/short highlights
                    title = "Weekly Newsletter: The AI Advantage",
                    contentOrUrl = "Explore how generative AI is transforming development streams, from fast layout builders to auto-tag triggers.",
                    notes = "Gmail Subscriptions • Tech Digest",
                    tags = "gmail, subscriptions, tech, ai",
                    timestamp = System.currentTimeMillis() - 3 * 3600000, // 3h ago
                    hasDeadline = false
                )
            )

            // Dynamic Seeding of Instagram Reels & Dev Guides
            vaultDao.insertItem(
                VaultItem(
                    contentType = "REEL",
                    title = "10 Android Studio Dev Shortcuts! 💻",
                    contentOrUrl = "https://www.instagram.com/reel/C7mXa88sAndroidStudio",
                    notes = "Super useful shortcuts for daily development! Especially multi-cursor edit (Alt+Shift+Click) and Extract Method.",
                    tags = "android, coding, developer, productivity",
                    timestamp = System.currentTimeMillis() - 86400000 * 3 // 3 days ago
                )
            )

            // YouTube Short
            vaultDao.insertItem(
                VaultItem(
                    contentType = "SHORT",
                    title = "60-Second Kotlin Coroutines Flow Guide 💡",
                    contentOrUrl = "https://youtube.com/shorts/kotlinFlowsColdHot",
                    notes = "Simple visualization showing the difference between Cold (Flow) and Hot (SharedFlow/StateFlow) streams.",
                    tags = "kotlin, flow, coroutines, advanced",
                    timestamp = System.currentTimeMillis() - 86400000 * 5 // 5 days ago
                )
            )
        }
    }
}
