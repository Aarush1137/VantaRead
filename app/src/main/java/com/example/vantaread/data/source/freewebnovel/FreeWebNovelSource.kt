package com.example.vantaread.data.source.freewebnovel

import android.content.Context
import com.example.vantaread.data.model.Chapter
import com.example.vantaread.data.model.Novel
import com.example.vantaread.data.model.NovelDetails
import com.example.vantaread.data.source.NovelSource
import com.example.vantaread.data.source.SourceCatalog
import com.example.vantaread.data.source.util.WebViewScraper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class FreeWebNovelSource(private val context: Context) : NovelSource {

    override val sourceId: String = SourceCatalog.FREE_WEB_NOVEL
    override val sourceName: String = "FreeWebNovel"
    private val baseUrl = "https://freewebnovel.com"
    private suspend fun fetch(url: String): Document {
        return withContext(Dispatchers.IO) {
            val doc = WebViewScraper.getHtml(context, url)
            doc
        }
    }

    override suspend fun searchNovels(query: String): List<Novel> {
        val url = "$baseUrl/search?searchkey=${query.replace(" ", "+")}"
        val doc = fetch(url)
        return doc.select(".li-row").mapNotNull { el ->
            val titleEl = el.select(".tit a").first() ?: return@mapNotNull null
            val link = titleEl.attr("href")
            Novel(
                title = titleEl.text(),
                url = if (link.startsWith("http")) link else "$baseUrl$link",
                coverUrl = el.select(".pic img").attr("src"),
                sourceId = sourceId
            )
        }
    }

    override suspend fun getPopularNovels(): List<Novel> {
        val url = "$baseUrl/most-popular-novels/"
        val doc = fetch(url)
        return doc.select(".li-row").mapNotNull { el ->
            val titleEl = el.select(".tit a").first() ?: return@mapNotNull null
            val link = titleEl.attr("href")
            Novel(
                title = titleEl.text(),
                url = if (link.startsWith("http")) link else "$baseUrl$link",
                coverUrl = el.select(".pic img").attr("src"),
                sourceId = sourceId
            )
        }
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails {
        val doc = fetch(novelUrl)
        val title = doc.select("h1.tit").text()
        val cover = doc.select(".pic img").attr("src")
        val author = doc.select("[title=Author]").text()
        val status = doc.select("[title=Status]").text()
        val summary = doc.select(".inner").text()
        val genres = doc.select("[title=Genre] a").map { it.text() }

        return NovelDetails(
            title = title,
            url = novelUrl,
            coverUrl = cover,
            author = author,
            synopsis = summary,
            status = status,
            genres = genres,
            latestUpdate = ""
        )
    }

    override suspend fun getChapterList(novelUrl: String): List<Chapter> {
        val doc = fetch(novelUrl)
        return doc.select("#m-chapters li a").mapIndexed { index, el ->
            val link = el.attr("href")
            Chapter(
                title = el.text(),
                url = if (link.startsWith("http")) link else "$baseUrl$link",
                novelUrl = novelUrl,
                index = index
            )
        }
    }

    override suspend fun getChapterContent(chapterUrl: String): String {
        val doc = fetch(chapterUrl)
        val content = doc.select(".txt").html()
        return content.replace("<p>", "").replace("</p>", "\n\n")
            .replace("<br>", "\n")
            .replace(Regex("<[^>]*>"), "")
    }
}
