package com.example.vantaread.data.source.novelfull

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.util.WebViewScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
class NovelFullSource(private val context: Context) : NovelSource {
    override val sourceId: String = "novelfull"
    override val sourceName: String = "NovelFull"

    private val baseUrl = "https://novelfull.com"

    private fun absoluteUrl(url: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            else -> "$baseUrl$url"
        }
    }

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/most-popular"
        val doc = runCatching { 
            Jsoup.connect(url).userAgent(userAgent).referrer(baseUrl).timeout(10000).get() 
        }.getOrElse { WebViewScraper.getHtml(context, url) }
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".list-truyen .row")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.truyen-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img.cover, img[src]")?.attr("src")?.let(::absoluteUrl) ?: ""
            
            novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl, sourceId = sourceId))
        }
        
        novels
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?keyword=${URLEncoder.encode(query, "UTF-8")}"
        val doc = runCatching { 
            Jsoup.connect(url).userAgent(userAgent).referrer(baseUrl).timeout(10000).get() 
        }.getOrElse { WebViewScraper.getHtml(context, url) }
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".list-truyen .row")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.truyen-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img.cover, img[src]")?.attr("src")?.let(::absoluteUrl) ?: ""
            
            novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl, sourceId = sourceId))
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val title = doc.selectFirst("h3.title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.title()
        val coverUrl = doc.selectFirst(".book img, meta[property=og:image]")
            ?.let { it.attr("src").ifBlank { it.attr("content") } }
            ?.let(::absoluteUrl)
            ?: ""
        val synopsis = doc.selectFirst(".desc-text")?.text() ?: ""
        val author = doc.selectFirst(".info a[href*=/author/]")?.text() ?: ""
        val genres = doc.select(".info a[href*=/genre/]").map { it.text() }
        val status = doc.select(".info div, .info li").firstOrNull { it.text().contains("Status", ignoreCase = true) }?.text()
            ?.substringAfter("Status", "")
            ?.trim()
            ?: ""
        
        NovelDetails(
            url = novelUrl,
            title = title,
            coverUrl = coverUrl,
            synopsis = synopsis,
            author = author,
            genres = genres,
            status = status,
            latestUpdate = ""
        )
    }

    override suspend fun getChapterList(novelUrl: String): List<Chapter> = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val chapters = mutableListOf<Chapter>()
        
        val novelId = doc.selectFirst("#truyen-id")?.attr("value")
            ?: doc.selectFirst("[data-novel-id]")?.attr("data-novel-id")
            
        if (novelId != null && novelId.isNotEmpty()) {
            val ajaxUrl = "$baseUrl/ajax/chapter-archive?novelId=$novelId"
            val ajaxDoc = WebViewScraper.getHtml(context, ajaxUrl)
            
            ajaxDoc.select("ul.list-chapter li a, .panel-body li a").forEachIndexed { index, element ->
                val url = absoluteUrl(element.attr("href"))
                val title = element.text()
                chapters.add(Chapter(url, novelUrl, title, index))
            }
        }
        
        // Fallback to pagination or first page
        if (chapters.isEmpty()) {
            val elements = doc.select("ul.list-chapter li a")
            elements.forEachIndexed { index, element ->
                val url = absoluteUrl(element.attr("href"))
                val title = element.text()
                chapters.add(Chapter(url, novelUrl, title, index))
            }
        }
        
        chapters
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, chapterUrl)
        
        // Find the main content text
        val contentElement = doc.selectFirst("#chapter-content")
        
        // Remove ads or junk if needed
        contentElement?.select(".ads, script")?.remove()
        
        contentElement?.html() ?: "Failed to load chapter content."
    }
}
