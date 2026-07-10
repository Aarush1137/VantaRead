package com.example.vantaread.data.source.lightnovelpub

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.util.WebViewScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

class LightNovelPubSource(private val context: Context) : NovelSource {
    override val sourceId: String = "lightnovelpub"
    override val sourceName: String = "LightNovelPub"

    private val baseUrl = "https://lightnovelpub.me"

    private fun absoluteUrl(url: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("http") -> url
            else -> "$baseUrl$url"
        }
    }

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/list/most-popular-novels/"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".ul-list1 .li-row, .novel-item")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.tit a, .novel-title a") ?: continue
            val title = titleElement.text().trim()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img")?.attr("src")?.let(::absoluteUrl) ?: ""
            
            if (title.isNotEmpty()) {
                novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl))
            }
        }
        
        novels.take(20)
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?keyword=${URLEncoder.encode(query, "UTF-8")}"
        val doc = WebViewScraper.getHtml(context, url)
        
        val novels = mutableListOf<Novel>()
        val items = doc.select(".ul-list1 .li-row, .novel-item")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.tit a, .novel-title a") ?: continue
            val title = titleElement.text().trim()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img")?.attr("src")?.let(::absoluteUrl) ?: ""
            
            if (title.isNotEmpty()) {
                novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl))
            }
        }
        
        novels
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val title = doc.selectFirst(".m-desc h1.tit, .novel-title")?.text() ?: doc.title()
        val coverUrl = doc.selectFirst(".m-book1 .pic img, .fixed-img img")?.attr("src")?.let(::absoluteUrl)
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: ""
        val synopsis = doc.selectFirst(".m-desc .inner, .m-desc .txt, .summary .content")?.text() ?: ""
        val author = doc.selectFirst(".m-book1 a[href*=/author/], .author a")?.text() ?: ""
        val status = doc.selectFirst(".m-book1 .glyphicon-time + .right .s1, .header-stats span strong")?.text() ?: ""
        
        val genres = doc.select(".m-book1 a[href*=/genres/], .categories a").map { it.text() }
        
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
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val chapters = mutableListOf<Chapter>()
        val elements = doc.select("a.chapter[href*=/chapter-], .ul-list-chapter a[href*=/chapter-], a[href*=/chapter-]")
        
        elements.forEachIndexed { index, element ->
            val url = absoluteUrl(element.attr("href"))
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
