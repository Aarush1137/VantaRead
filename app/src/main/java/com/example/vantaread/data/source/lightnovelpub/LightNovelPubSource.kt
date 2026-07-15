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
import org.jsoup.nodes.Document
import java.net.URLEncoder

class LightNovelPubSource(private val context: Context) : NovelSource {
    override val sourceId: String = "lightnovelpub"
    override val sourceName: String = "LightNovelPub"

    private val baseUrl = "https://lightnovelpub.me"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    private suspend fun fetchDocument(url: String): Document {
        return WebViewScraper.getHtml(context, url)
    }

    private fun absoluteUrl(url: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("http") -> url
            else -> "$baseUrl$url"
        }
    }

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/list/most-popular-novels/"
        val doc = fetchDocument(url)
        
        parseNovelList(doc).take(20)
    }

    private fun parseNovelList(doc: Document): List<Novel> {
        val novels = mutableListOf<Novel>()
        val items = doc.select(".ul-list1 .li-row, .novel-item")
        
        for (item in items) {
            val titleElement = item.selectFirst("h3.tit a, .novel-title a") ?: continue
            val title = titleElement.text().trim()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img")?.attr("src")?.let(::absoluteUrl) ?: ""
            
            if (title.isNotEmpty()) {
                novels.add(Novel(url = novelUrl, title = title, coverUrl = coverUrl, sourceId = sourceId))
            }
        }

        return novels
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/search?keyword=${URLEncoder.encode(query, "UTF-8")}"
        val doc = runCatching { 
            Jsoup.connect(url).userAgent(userAgent).referrer(baseUrl).timeout(10000).get() 
        }.getOrElse { fetchDocument(url) }
        
        val searchResults = parseNovelList(doc)
        if (searchResults.isNotEmpty()) return@withContext searchResults

        val normalizedQuery = query.trim().lowercase()
        
        val popularDoc = runCatching { 
            Jsoup.connect("$baseUrl/list/most-popular-novels/").userAgent(userAgent).referrer(baseUrl).timeout(10000).get() 
        }.getOrElse { fetchDocument("$baseUrl/list/most-popular-novels/") }
        
        parseNovelList(popularDoc)
            .filter { it.title.lowercase().contains(normalizedQuery) }
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = fetchDocument(novelUrl)
        
        val title = doc.selectFirst("meta[property=og:novel:novel_name]")?.attr("content")
            ?: doc.selectFirst(".m-desc h1.tit, .novel-title")?.text()
            ?: doc.title()
        val coverUrl = doc.selectFirst(".m-book1 .pic img, .fixed-img img")?.attr("src")?.let(::absoluteUrl)
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: ""
        val synopsis = doc.selectFirst(".m-desc .inner, .m-desc .txt, .summary .content")?.text() ?: ""
        val author = doc.selectFirst("meta[property=og:novel:author]")?.attr("content")
            ?: doc.selectFirst(".m-book1 a[href*=/author/], .author a")?.text()
            ?: ""
        val status = doc.selectFirst("meta[property=og:novel:status]")?.attr("content")
            ?: doc.selectFirst(".m-book1 .glyphicon-time + .right .s1, .header-stats span strong")?.text()
            ?: ""
        
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
        val doc = runCatching { 
            Jsoup.connect(novelUrl).userAgent(userAgent).referrer(baseUrl).timeout(10000).get() 
        }.getOrElse { fetchDocument(novelUrl) }
        
        val headerText = doc.selectFirst(".header-stats, .novel-info, .m-book1")?.text() ?: ""
        
        // Match "1,234 Chapters", "Chapters: 1234", "1234", etc. specifically inside the strong tag if possible
        val chapterCountRaw = doc.select(".header-stats span:contains(Chapters) strong, .header-stats strong, .novel-info strong").map { it.text() }
            .plus(headerText)
            .joinToString(" ")
            
        val chapterMatch = Regex("([0-9,]+)\\s*Chapters?", RegexOption.IGNORE_CASE).find(chapterCountRaw)
            ?: Regex("Chapters?\\s*:\\s*([0-9,]+)", RegexOption.IGNORE_CASE).find(chapterCountRaw)
            
        val chapterCount = chapterMatch?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()
        
        if (chapterCount != null && chapterCount > 0) {
            val chaptersUrlBase = novelUrl.trimEnd('/')
            return@withContext (1..chapterCount).map { index ->
                Chapter(
                    url = "$chaptersUrlBase/chapter-$index",
                    novelUrl = novelUrl,
                    title = "Chapter $index",
                    index = index - 1
                )
            }
        }
        
        // Fallback to parsing the chapters page directly
        val chaptersUrl = if (novelUrl.endsWith("/chapters")) novelUrl else "${novelUrl.trimEnd('/')}/chapters"
        val chaptersDoc = fetchDocument(chaptersUrl)
        parseChapterPage(chaptersDoc, novelUrl, 0)
    }

    override suspend fun getChapterPage(novelUrl: String, page: Int): List<Chapter> = withContext(Dispatchers.IO) {
        val chaptersUrl = if (novelUrl.endsWith("/chapters")) novelUrl else "${novelUrl.trimEnd('/')}/chapters"
        val pageUrl = if (page <= 0) chaptersUrl else "$chaptersUrl/page-${page + 1}"
        val doc = fetchDocument(pageUrl)
        parseChapterPage(doc, novelUrl, page)
    }

    private fun parseChapterPage(doc: Document, novelUrl: String, page: Int): List<Chapter> {
        return doc.select(".m-newest2 a[href*=/chapter], .ul-list-chapter a[href*=/chapter], a.chapter[href*=/chapter]")
            .mapNotNull { element ->
                val url = absoluteUrl(element.attr("href"))
                val title = element.select(".chapter-title").text().ifEmpty { element.attr("title").ifEmpty { element.text() } }.trim()
                if (title.isNotEmpty()) Chapter(url, novelUrl, title, 0) else null
            }
            .distinctBy { it.url }
            .mapIndexed { index, chapter -> chapter.copy(index = page * 40 + index) }
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = fetchDocument(chapterUrl)
        
        val contentElement = doc.selectFirst("#chapter-container")
        
        // Clean junk
        contentElement?.select(".adsbox, .ad-container, script, style")?.remove()
        
        contentElement?.html() ?: "Failed to load chapter content from LightNovelPub."
    }
}
