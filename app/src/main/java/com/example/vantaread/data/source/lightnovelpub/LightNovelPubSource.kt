package com.example.vantaread.data.source.lightnovelpub

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.util.WebViewScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class LightNovelPubSource(private val context: Context) : NovelSource {
    override val sourceId: String = "lightnovelpub"
    override val sourceName: String = "LightNovelPub"

    private val baseUrl = "https://www.lightnovelpub.com"

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/browse/all/popular/all"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".novel-item")
        
        for (item in items) {
            val titleElement = item.selectFirst(".novel-title a") ?: continue
            val title = titleElement.text().trim()
            val novelUrl = titleElement.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val coverUrl = item.selectFirst("img")?.attr("src")?.let { if (it.startsWith("http")) it else "$baseUrl$it" } ?: ""
            
            if (title.isNotEmpty()) {
                novels.add(Novel(novelUrl, title, coverUrl, sourceId))
            }
        }
        
        novels.take(20)
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?keyword=${query.replace(" ", "+")}"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".novel-item")
        
        for (item in items) {
            val titleElement = item.selectFirst(".novel-title a") ?: continue
            val title = titleElement.text().trim()
            val novelUrl = titleElement.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val coverUrl = item.selectFirst("img")?.attr("src")?.let { if (it.startsWith("http")) it else "$baseUrl$it" } ?: ""
            
            if (title.isNotEmpty()) {
                novels.add(Novel(novelUrl, title, coverUrl, sourceId))
            }
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val title = doc.selectFirst(".novel-title")?.text() ?: ""
        val coverUrl = doc.selectFirst(".fixed-img img")?.attr("src")?.let { if (it.startsWith("http")) it else "$baseUrl$it" } ?: ""
        val synopsis = doc.selectFirst(".summary .content")?.text() ?: ""
        val author = doc.selectFirst(".author a")?.text() ?: ""
        val status = doc.selectFirst(".header-stats span strong")?.text() ?: ""
        
        val genres = doc.select(".categories a").map { it.text() }
        
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
        // LightNovelPub usually has a /chapters endpoint or lists them on the main page.
        // We'll try the chapters page if available.
        val chaptersUrl = if (novelUrl.endsWith("/chapters")) novelUrl else "$novelUrl/chapters"
        val doc = WebViewScraper.getHtml(context, chaptersUrl)
        
        val chapters = mutableListOf<Chapter>()
        val elements = doc.select(".chapter-list li a")
        
        elements.forEachIndexed { index, element ->
            val url = element.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val title = element.select(".chapter-title").text().ifEmpty { element.text() }.trim()
            if (title.isNotEmpty()) {
                chapters.add(Chapter(url, novelUrl, title, index))
            }
        }
        
        chapters
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, chapterUrl)
        
        val contentElement = doc.selectFirst("#chapter-container")
        
        // Clean junk
        contentElement?.select(".adsbox, .ad-container, script, style")?.remove()
        
        contentElement?.html() ?: "Failed to load chapter content from LightNovelPub."
    }
}
