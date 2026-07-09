package com.example.vantaread.data.source.wtrlab

import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

class WtrLabSource : NovelSource {
    override val sourceId = "wtr-lab"
    override val sourceName = "WTR Lab"
    private val baseUrl = "https://wtr-lab.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val searchUrl = "$baseUrl/en/series?search=$query"
        val doc = Jsoup.connect(searchUrl).userAgent(userAgent).get()
        
        val novels = mutableListOf<Novel>()
        // TODO: Selectors need adjustment based on actual wtr-lab.com HTML
        val elements = doc.select(".series-item, .search-result-item") // Placeholder
        
        for (element in elements) {
            val titleElement = element.selectFirst("a")
            val url = titleElement?.attr("href")?.let { if (it.startsWith("http")) it else "$baseUrl$it" } ?: continue
            val title = titleElement.text()
            val coverUrl = element.selectFirst("img")?.attr("src") ?: ""
            val author = element.selectFirst(".author")?.text() ?: ""
            val status = element.selectFirst(".status")?.text() ?: ""
            
            novels.add(Novel(url, title, coverUrl, author = author, status = status))
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(novelUrl).userAgent(userAgent).get()
            
        // TODO: Selectors need adjustment
        val title = doc.selectFirst("h1")?.text() ?: ""
        val coverUrl = doc.selectFirst(".cover img, img.cover")?.attr("src") ?: ""
        val synopsis = doc.selectFirst(".synopsis, .description")?.text() ?: ""
        val author = doc.selectFirst(".author")?.text() ?: ""
        val status = doc.selectFirst(".status")?.text() ?: ""
        val latestUpdate = doc.selectFirst(".latest-update, .updated-at")?.text() ?: ""
        
        val genres = doc.select(".genres a, .tags a").map { it.text() }
        
        NovelDetails(novelUrl, title, coverUrl, synopsis, author, genres, status, latestUpdate)
    }

    override suspend fun getChapterList(novelUrl: String): List<Chapter> = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(novelUrl).userAgent(userAgent).get()
            
        val chapters = mutableListOf<Chapter>()
        // TODO: Selectors need adjustment
        val elements = doc.select(".chapter-list a, .chapters a")
        
        elements.forEachIndexed { index, element ->
            val url = element.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val title = element.text()
            chapters.add(Chapter(url, novelUrl, title, index))
        }
        
        chapters
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(chapterUrl).userAgent(userAgent).get()
            
        // TODO: Selectors need adjustment
        val contentElement = doc.selectFirst(".chapter-content, .text-content")
        
        // Preserve basic HTML formatting and images as per requirements
        val safelist = Safelist.none()
            .addTags("b", "i", "br", "img", "p", "div", "strong", "em")
            .addAttributes("img", "src", "alt")
            
        contentElement?.let { Jsoup.clean(it.html(), safelist) } ?: ""
    }
}
