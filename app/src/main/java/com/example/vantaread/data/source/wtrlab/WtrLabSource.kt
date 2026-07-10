package com.example.vantaread.data.source.wtrlab

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

class WtrLabSource(private val context: Context) : NovelSource {
    override val sourceId = "wtr-lab"
    override val sourceName = "WTR Lab"
    private val baseUrl = "https://wtr-lab.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en/"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val elements = doc.select("a[href^=/en/novel/]:has(img)")
        
        for (element in elements) {
            val novelUrl = element.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val imgElement = element.selectFirst("img")
            val coverUrl = imgElement?.attr("src")?.let { if (it.startsWith("http")) it else "$baseUrl$it" } ?: ""
            val title = imgElement?.attr("alt") ?: ""
            
            if (title.isNotEmpty() && novels.none { it.url == novelUrl }) {
                novels.add(Novel(novelUrl, title, coverUrl, author = "", status = ""))
            }
        }
        
        novels.take(12)
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val searchUrl = "$baseUrl/en/novel-list?search=$query"
        val doc = WebViewScraper.getHtml(context, searchUrl)
        
        val novels = mutableListOf<Novel>()
        val elements = doc.select("a[href^=/en/novel/]:has(img)")
        
        for (element in elements) {
            val url = element.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val imgElement = element.selectFirst("img")
            val title = imgElement?.attr("alt") ?: ""
            val coverUrl = imgElement?.attr("src")?.let { if (it.startsWith("http")) it else "$baseUrl$it" } ?: ""
            
            if (title.isNotEmpty() && novels.none { it.url == url }) {
                novels.add(Novel(url, title, coverUrl, author = "", status = ""))
            }
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
            
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
        val doc = WebViewScraper.getHtml(context, novelUrl)
            
        val chapters = mutableListOf<Chapter>()
        val uniqueUrls = mutableSetOf<String>()
        
        // Find any link that points to a chapter for this novel
        val elements = doc.select("a[href*=/chapter]")
        
        for (element in elements) {
            val url = element.attr("href").let { if (it.startsWith("http")) it else "$baseUrl$it" }
            val title = element.text().trim()
            
            // Only add if it's a valid title and we haven't added this chapter URL yet
            if (title.isNotEmpty() && uniqueUrls.add(url)) {
                chapters.add(Chapter(url, novelUrl, title, chapters.size))
            }
        }
        
        // Reverse if they are sorted newest first, but usually WTR lab lists them in order or we can just rely on the page order.
        chapters
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, chapterUrl)
            
        // TODO: Selectors need adjustment
        val contentElement = doc.selectFirst(".chapter-content, .text-content")
        
        // Preserve basic HTML formatting and images as per requirements
        val safelist = Safelist.none()
            .addTags("b", "i", "br", "img", "p", "div", "strong", "em")
            .addAttributes("img", "src", "alt")
            
        contentElement?.let { Jsoup.clean(it.html(), safelist) } ?: ""
    }
}
