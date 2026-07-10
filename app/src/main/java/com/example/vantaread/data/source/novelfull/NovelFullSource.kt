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

class NovelFullSource(private val context: Context) : NovelSource {
    override val sourceId: String = "novelfull"
    override val sourceName: String = "NovelFull"

    private val baseUrl = "https://novelfull.com"

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/most-popular"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".list-truyen .row")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.truyen-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = baseUrl + titleElement.attr("href")
            val coverUrl = baseUrl + (item.selectFirst("img.cover")?.attr("src") ?: "")
            
            novels.add(Novel(novelUrl, title, coverUrl, sourceId))
        }
        
        novels
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?keyword=${query.replace(" ", "+")}"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".list-truyen .row")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.truyen-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = baseUrl + titleElement.attr("href")
            val coverUrl = baseUrl + (item.selectFirst("img.cover")?.attr("src") ?: "")
            
            novels.add(Novel(novelUrl, title, coverUrl, sourceId))
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val title = doc.selectFirst("h3.title")?.text() ?: ""
        val coverUrl = baseUrl + (doc.selectFirst(".book img")?.attr("src") ?: "")
        val synopsis = doc.selectFirst(".desc-text")?.text() ?: ""
        
        NovelDetails(
            url = novelUrl,
            title = title,
            coverUrl = coverUrl,
            synopsis = synopsis,
            author = "",
            genres = emptyList(),
            status = "",
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
            val url = baseUrl + element.attr("href")
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
