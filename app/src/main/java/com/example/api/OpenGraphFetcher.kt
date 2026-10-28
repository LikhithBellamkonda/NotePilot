package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object OpenGraphFetcher {
    suspend fun fetch(url: String): OpenGraphData? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).timeout(5000).get()

            var title = doc.select("meta[property=og:title]").attr("content")
            if (title.isNullOrBlank()) title = doc.title()

            var description = doc.select("meta[property=og:description]").attr("content")
            if (description.isNullOrBlank()) description = doc.select("meta[name=description]").attr("content")

            var imageUrl = doc.select("meta[property=og:image]").attr("content")

            if (title.isNullOrBlank() && description.isNullOrBlank() && imageUrl.isNullOrBlank()) {
                return@withContext null
            }
            OpenGraphData(title, description, imageUrl)
        } catch (e: Exception) {
            android.util.Log.e("OpenGraphFetcher", "Failed to fetch open graph data for $url", e)
            null
        }
    }
}
