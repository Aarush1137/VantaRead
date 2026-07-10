package com.example.vantaread.data.source.wtrlab

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.util.WebViewScraper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder
import javax.inject.Inject

class WtrLabSource @Inject constructor(
    @ApplicationContext private val context: Context
) : NovelSource {
    override val sourceId: String = "wtrlab"
    override val sourceName: String = "WTR-Lab"
    private val baseUrl = "https://wtr-lab.com"

    override suspend fun searchNovels(query: String): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en/search?title=${URLEncoder.encode(query, "UTF-8")}"
        val doc = WebViewScraper.getHtml(context, url)
        
        doc.select("a[href*=/novel/]").mapNotNull { element ->
            val novelUrl = element.attr("href").let { if (it.startsWith("http")) it else baseUrl + it }
            // Wtr-Lab uses specific formatting, we try to extract the title and img
            val titleElement = element.selectFirst("div > span:first-child, h3, .title") ?: element
            val title = titleElement.text().trim()
            val imgElement = element.selectFirst("img")
            
            if (title.isNotEmpty() && !novelUrl.contains("/chapter-")) {
                Novel(
                    url = novelUrl,
                    title = title,
                    coverUrl = imgElement?.attr("src")?.let { if (it.startsWith("http")) it else baseUrl + it } ?: ""
                )
            } else {
                null
            }
        }.distinctBy { it.url }
    }

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en"
        val doc = WebViewScraper.getHtml(context, url)
        
        doc.select("a[href*=/novel/]").mapNotNull { element ->
            val novelUrl = element.attr("href").let { if (it.startsWith("http")) it else baseUrl + it }
            val title = element.attr("title").ifEmpty { element.text() }.trim()
            val imgElement = element.selectFirst("img")
            
            if (title.isNotEmpty() && !novelUrl.contains("/chapter-")) {
                Novel(
                    url = novelUrl,
                    title = title,
                    coverUrl = imgElement?.attr("src")?.let { if (it.startsWith("http")) it else baseUrl + it } ?: ""
                )
            } else null
        }.distinctBy { it.url }.take(20)
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, novelUrl)
        
        val title = doc.selectFirst("h1")?.text() ?: "Unknown Title"
        val coverUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst("img[alt*=$title]")?.attr("src") ?: ""
            
        val synopsis = doc.selectFirst(".desc-wrap, .description")?.text() ?: "No synopsis available."
        
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
        val doc = WebViewScraper.getHtml(context, novelUrl)
        val chapters = mutableListOf<Chapter>()
        
        // Find links that look like chapters
        doc.select("a[href*=/chapter-]").forEachIndexed { index, element ->
            chapters.add(
                Chapter(
                    url = if (element.attr("href").startsWith("http")) element.attr("href") else baseUrl + element.attr("href"),
                    novelUrl = novelUrl,
                    title = element.text().ifEmpty { "Chapter ${index + 1}" },
                    index = index
                )
            )
        }
        
        // Filter out duplicate chapter links (WTR-Lab might have multiple links to the same chapter)
        chapters.distinctBy { it.url }.mapIndexed { index, chapter -> 
            chapter.copy(index = index)
        }
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = WebViewScraper.getHtml(context, chapterUrl)
        
        // WTR-Lab chapter content
        val contentElement = doc.selectFirst("article, .chapter-content, #chapter-content")
        
        contentElement?.select("script, style, .adsbox")?.remove()
        
        contentElement?.html() ?: "Failed to extract chapter content."
    }
}
