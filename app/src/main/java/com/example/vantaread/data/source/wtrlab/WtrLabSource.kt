package com.example.vantaread.data.source.wtrlab

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import javax.inject.Inject

class WtrLabSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : NovelSource {
    override val sourceId: String = "wtrlab"
    override val sourceName: String = "WTR-Lab"
    private val baseUrl = "https://wtr-lab.com"
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"

    private suspend fun fetchDocument(url: String): Document {
        return com.example.vantaread.data.source.util.WebViewScraper.getHtml(context, url)
    }

    private fun absoluteUrl(url: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("http") -> url
            else -> "$baseUrl$url"
        }
    }

    private fun parseNovelCards(doc: org.jsoup.nodes.Document): List<Novel> {
        return doc.select("a[href*=/en/novel/]").mapNotNull { element ->
            val title = element.attr("title").ifEmpty { element.text() }.trim()
            val href = element.attr("href")
            val novelUrl = absoluteUrl(href)
            val card = element.parents().firstOrNull { it.selectFirst("img") != null } ?: element.parent()
            val imgElement = card?.selectFirst("img[alt], img[src]")
            val coverUrl = imgElement?.attr("src")?.let(::absoluteUrl) ?: ""

            if (title.isNotEmpty() && novelUrl.contains("/en/novel/") && !novelUrl.contains("/chapter-")) {
                Novel(
                    url = novelUrl,
                    title = title,
                    coverUrl = coverUrl,
                    sourceId = sourceId
                )
            } else {
                null
            }
        }.distinctBy { it.url }
    }

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en/novel-list"
        val doc = fetchDocument(url)

        val normalizedQuery = query.trim().lowercase()
        parseNovelCards(doc).filter { novel ->
            novel.title.lowercase().contains(normalizedQuery)
        }
    }

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en/novel-list"
        val doc = fetchDocument(url)

        parseNovelCards(doc).take(20)
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = fetchDocument(novelUrl)
        
        val title = doc.selectFirst("h1")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.removePrefix("Read ")?.substringBefore(" RAW English Translation")
            ?: "Unknown Title"
        val coverUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst("img[alt*=$title]")?.attr("src")?.let(::absoluteUrl) ?: ""
            
        val synopsis = doc.selectFirst(".desc-wrap, .description")?.text()
            ?: doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: "No synopsis available."
        
        var author = "Unknown"
        var status = "Unknown"
        val genres = mutableListOf<String>()
        
        val authorElement = doc.selectFirst("a[href^=/en/author/]")
        if (authorElement != null) {
            author = authorElement.text()
        }
        
        val statusSpan = doc.select("span[translate=no]:contains(Status)").firstOrNull()
        if (statusSpan != null) {
            status = statusSpan.nextElementSibling()?.text() ?: "Unknown"
        }
        
        // Extract genres from __NEXT_DATA__ if possible, or just raw text
        doc.select("a[href*=/genre/]").forEach { genreElement ->
            genres.add(genreElement.text())
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
        
        // Find links that look like chapters
        doc.select("a[href*=/chapter-]").forEachIndexed { index, element ->
            chapters.add(
                Chapter(
                    url = absoluteUrl(element.attr("href")),
                    novelUrl = novelUrl,
                    title = element.text().ifEmpty { "Chapter ${index + 1}" },
                    index = index
                )
            )
        }
        
        val distinctChapters = chapters.distinctBy { it.url }.mapIndexed { index, chapter ->
            chapter.copy(index = index)
        }

        if (distinctChapters.isNotEmpty()) {
            return@withContext distinctChapters
        }

        val chapterCount = Regex(""""(?:raw_)?chapter_count":(\d+)""")
            .find(doc.html())
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?: 0

        (1..chapterCount).map { chapterNumber ->
            Chapter(
                url = "$novelUrl/chapter-$chapterNumber",
                novelUrl = novelUrl,
                title = "Chapter $chapterNumber",
                index = chapterNumber - 1
            )
        }
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = fetchDocument(chapterUrl)
        
        // WTR-Lab chapter content
        val contentElement = doc.selectFirst("article, .chapter-content, #chapter-content")
        
        contentElement?.select("script, style, .adsbox")?.remove()
        
        contentElement?.html() ?: "Failed to extract chapter content."
    }
}
