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

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/most-popular"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".list-truyen .row")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.truyen-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img.cover, img[src]")?.attr("src")?.let(::absoluteUrl) ?: ""
            
            novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl))
        }
        
        novels
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?keyword=${URLEncoder.encode(query, "UTF-8")}"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".list-truyen .row")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.truyen-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img.cover, img[src]")?.attr("src")?.let(::absoluteUrl) ?: ""
            
            novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl))
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
        
        // NovelFull shows chapters using pagination, but often has a "show all" or we can parse the list.
        // For simplicity, we just grab the chapters visible on the first page or the chapter list fragment.
        val elements = doc.select("ul.list-chapter li a")
        
        elements.forEachIndexed { index, element ->
            val url = absoluteUrl(element.attr("href"))
            val title = element.text()
            chapters.add(Chapter(url, novelUrl, title, index))
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
