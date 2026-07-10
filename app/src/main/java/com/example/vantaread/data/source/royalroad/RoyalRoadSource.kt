package com.example.vantaread.data.source.royalroad

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.util.WebViewScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

class RoyalRoadSource(private val context: Context) : NovelSource {
    override val sourceId = "royalroad"
    override val sourceName = "Royal Road"
    private val baseUrl = "https://www.royalroad.com"

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/fictions/best-rated"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".fiction-list-item")
        
        for (item in items) {
            val titleElement = item.selectFirst(".fiction-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = baseUrl + titleElement.attr("href")
            val coverUrl = item.selectFirst("img[src]")?.attr("src")?.let { 
                if (it.startsWith("http")) it else "$baseUrl$it" 
            } ?: ""
            
            novels.add(Novel(novelUrl, title, coverUrl, author = "", status = ""))
        }
        
        novels
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/fictions/search?title=${query.replace(" ", "+")}"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".fiction-list-item")
        
        for (item in items) {
            val titleElement = item.selectFirst(".fiction-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = baseUrl + titleElement.attr("href")
            val coverUrl = item.selectFirst("img[src]")?.attr("src")?.let { 
                if (it.startsWith("http")) it else "$baseUrl$it" 
            } ?: ""
            
            novels.add(Novel(novelUrl, title, coverUrl, author = "", status = ""))
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val title = doc.selectFirst("h1.font-white")?.text() ?: doc.title()
        val coverUrl = doc.selectFirst(".fic-header img")?.attr("src")?.let { 
                if (it.startsWith("http")) it else "$baseUrl$it" 
        } ?: ""
        val synopsis = doc.selectFirst(".description")?.text() ?: ""
        
        val author = doc.selectFirst(".fic-header h4 a")?.text() ?: ""
        val genres = doc.select(".tags a.tags").map { it.text() }
        
        // Stats
        val statusList = doc.select(".stats-content .list-unstyled li")
        var status = ""
        for (li in statusList) {
            if (li.text().contains("Status")) {
                status = li.text().replace("Status", "").trim()
            }
        }
        
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
        val elements = doc.select("#chapters tbody tr")
        
        elements.forEachIndexed { index, tr ->
            val a = tr.selectFirst("a[href]") ?: return@forEachIndexed
            val url = baseUrl + a.attr("href")
            val title = a.text().trim()
            
            chapters.add(Chapter(url, novelUrl, title, index))
        }
        
        chapters
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, chapterUrl)
        
        val contentElement = doc.selectFirst(".chapter-content")
        
        val safelist = Safelist.none()
            .addTags("b", "i", "br", "p", "div", "strong", "em", "h1", "h2", "h3", "h4")
            
        contentElement?.let { Jsoup.clean(it.html(), safelist) } ?: "Failed to load chapter content."
    }
}
