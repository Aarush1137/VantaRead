package com.example.vantaread.data.source.boxnovel

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.util.WebViewScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class BoxNovelSource(private val context: Context) : NovelSource {
    override val sourceId: String = "boxnovel"
    override val sourceName: String = "BoxNovel"

    private val baseUrl = "https://boxnovel.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    private fun absoluteUrl(url: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("http") -> url
            url.startsWith("//") -> "https:$url"
            else -> "$baseUrl$url"
        }
    }

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/manga-tag/light-novel/page/1/?m_order=views"
        val doc = runCatching {
            Jsoup.connect(url).userAgent(userAgent).referrer(baseUrl).timeout(10000).get()
        }.getOrElse { WebViewScraper.getHtml(context, url) }

        doc.select(".page-item-detail").mapNotNull { item ->
            val titleElement = item.selectFirst(".post-title a") ?: return@mapNotNull null
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img")?.attr("src")?.let(::absoluteUrl) ?: ""
            Novel(url = novelUrl, title = title, coverUrl = coverUrl, sourceId = sourceId)
        }
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/?s=$encodedQuery&post_type=wp-manga"
        val doc = runCatching {
            Jsoup.connect(url).userAgent(userAgent).referrer(baseUrl).timeout(10000).get()
        }.getOrElse { WebViewScraper.getHtml(context, url) }

        doc.select(".c-tabs-item__content").mapNotNull { item ->
            val titleElement = item.selectFirst(".post-title a") ?: return@mapNotNull null
            val title = titleElement.text()
            val novelUrl = absoluteUrl(titleElement.attr("href"))
            val coverUrl = item.selectFirst("img")?.attr("src")?.let(::absoluteUrl) ?: ""
            Novel(url = novelUrl, title = title, coverUrl = coverUrl, sourceId = sourceId)
        }
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)

        val title = doc.selectFirst(".post-title h1")?.text() ?: ""
        val coverUrl = doc.selectFirst(".summary_image img")?.attr("src")?.let(::absoluteUrl) ?: ""
        val synopsis = doc.selectFirst(".description-summary")?.text() ?: ""
        val author = doc.selectFirst(".author-content a")?.text() ?: ""
        val genres = doc.select(".genres-content a").map { it.text() }
        val status = doc.select(".post-status .summary-content").lastOrNull()?.text() ?: ""

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
        
        // BoxNovel often loads chapters via AJAX or hidden in a list
        val chapters = doc.select(".wp-manga-chapter a").mapIndexed { index, element ->
            val url = absoluteUrl(element.attr("href"))
            val title = element.text()
            Chapter(url, novelUrl, title, index)
        }.reversed() // Usually listed latest first

        chapters.mapIndexed { index, chapter -> chapter.copy(index = index) }
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, chapterUrl)
        val contentElement = doc.selectFirst(".reading-content") ?: doc.selectFirst(".text-left")
        
        contentElement?.select(".ads, script, .code-block")?.remove()
        contentElement?.html() ?: "Failed to load chapter content."
    }
}
