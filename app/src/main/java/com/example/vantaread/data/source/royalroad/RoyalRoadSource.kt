package com.example.vantaread.data.source.royalroad

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import java.net.URLEncoder

class RoyalRoadSource(private val context: Context) : NovelSource {
    override val sourceId = "royalroad"
    override val sourceName = "Royal Road"
    private val baseUrl = "https://www.royalroad.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    private fun fetchDocument(url: String, referer: String = baseUrl): Document {
        return Jsoup.connect(url)
            .userAgent(userAgent)
            .referrer(referer)
            .timeout(30000)
            .get()
    }

    private fun absoluteUrl(url: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("http") -> url
            else -> "$baseUrl$url"
        }
    }

    private fun coverUrlFrom(item: org.jsoup.nodes.Element): String {
        val rawUrl = item.selectFirst("img[data-type=cover], img[src], img[data-src]")
            ?.let { img -> img.attr("src").ifBlank { img.attr("data-src") } }
            ?: ""
        return absoluteUrl(rawUrl)
    }

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/fictions/best-rated"
        val doc = fetchDocument(url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".fiction-list-item")
        
        for (item in items) {
            val titleElement = item.selectFirst(".fiction-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = coverUrlFrom(item)
            
            novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl, sourceId = sourceId))
        }
        
        novels
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/fictions/search?title=${URLEncoder.encode(query, "UTF-8")}"
        val doc = fetchDocument(url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".fiction-list-item")
        
        for (item in items) {
            val titleElement = item.selectFirst(".fiction-title a") ?: continue
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = coverUrlFrom(item)
            
            novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl, sourceId = sourceId))
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = fetchDocument(novelUrl)
        
        val title = doc.selectFirst("h1.font-white")?.text() ?: doc.title()
        val coverUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst(".fic-header img[data-type=cover], .fic-header img")?.attr("src")?.let(::absoluteUrl)
            ?: ""
        val synopsis = doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst(".description, .fiction-info .description")?.text()
            ?: ""
        
        val author = doc.selectFirst("meta[property=books:author]")?.attr("content")
            ?: doc.selectFirst(".fic-header h4 a")?.text()
            ?: ""
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
        val doc = fetchDocument(novelUrl)
        
        val chapters = mutableListOf<Chapter>()
        val elements = doc.select("#chapters tbody tr, tr.chapter-row")
        
        elements.forEachIndexed { index, tr ->
            val a = tr.selectFirst("a[href]") ?: return@forEachIndexed
            val url = absoluteUrl(a.attr("href"))
            val title = a.text().trim()
            
            chapters.add(Chapter(url, novelUrl, title, index))
        }
        
        chapters
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = fetchDocument(chapterUrl, referer = chapterUrl.substringBefore("/chapter/", missingDelimiterValue = baseUrl))
        
        val contentElement = doc.selectFirst(".chapter-content")
        
        val safelist = Safelist.none()
            .addTags("b", "i", "br", "p", "div", "strong", "em", "h1", "h2", "h3", "h4")
            
        contentElement?.let { Jsoup.clean(it.html(), safelist) } ?: "Failed to load chapter content."
    }
}
