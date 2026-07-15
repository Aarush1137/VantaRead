package com.example.vantaread.data.source.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object ScraperClient {
    const val DESKTOP_USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    suspend fun fetchDocument(
        context: Context?,
        url: String,
        referer: String,
        timeoutMs: Int = 30000
    ): Document {
        repeat(2) { attempt ->
            val doc = withContext(Dispatchers.IO) {
                runCatching {
                    Jsoup.connect(url)
                        .userAgent(DESKTOP_USER_AGENT)
                        .referrer(referer)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Cache-Control", "no-cache")
                        .followRedirects(true)
                        .ignoreHttpErrors(true)
                        .timeout(timeoutMs)
                        .get()
                }.getOrNull()
            }

            if (doc != null && !looksBlockedOrEmpty(doc)) {
                return doc
            }
            if (attempt == 0) delay(350)
        }

        if (context != null) {
            return WebViewScraper.getHtml(context, url)
        }

        return Jsoup.parse("<html><body>Failed to load $url</body></html>", url)
    }

    fun looksBlockedOrEmpty(doc: Document): Boolean {
        val text = doc.text()
        val title = doc.title()
        return text.length < 120 ||
            title.contains("Just a moment", ignoreCase = true) ||
            title.contains("Attention Required", ignoreCase = true) ||
            text.contains("Checking if the site connection is secure", ignoreCase = true) ||
            text.contains("enable JavaScript and cookies", ignoreCase = true) ||
            doc.select("#challenge-running, .cf-browser-verification, .challenge-error-text").isNotEmpty()
    }
}
