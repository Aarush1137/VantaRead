package com.example.vantaread.data.source.scribblehub

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

class ScribbleHubSource(private val context: Context) : NovelSource {

    override val sourceId: String = SourceCatalog.SCRIBBLE_HUB
    override val sourceName: String = "ScribbleHub"
    private val baseUrl = "https://www.scribblehub.com"
    private suspend fun fetch(url: String): Document {
        return withContext(Dispatchers.IO) {
            val doc = WebViewScraper.getHtml(context, url)
            doc
        }
    }

    override suspend fun searchNovels(query: String): List<Novel> {
        val url = "$baseUrl/?s=${query.replace(" ", "+")}&post_type=fictionposts"
        val doc = fetch(url)
        return doc.select(".search_main_box").mapNotNull { el ->
            val titleEl = el.select(".search_title a").first() ?: return@mapNotNull null
            Novel(
                title = titleEl.text(),
                url = titleEl.attr("href"),
                coverUrl = el.select(".search_img img").attr("src"),
                sourceId = sourceId
            )
        }
    }

    override suspend fun getPopularNovels(): List<Novel> {
        val url = "$baseUrl/series-ranking/"
        val doc = fetch(url)
        return doc.select(".search_main_box").mapNotNull { el ->
            val titleEl = el.select(".search_title a").first() ?: return@mapNotNull null
            Novel(
                title = titleEl.text(),
                url = titleEl.attr("href"),
                coverUrl = el.select(".search_img img").attr("src"),
                sourceId = sourceId
            )
        }
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails {
        val doc = fetch(novelUrl)
        val title = doc.select(".fic_title").text()
        val cover = doc.select(".fic_image img").attr("src")
        val author = doc.select(".auth_name_fic").text()
        val summary = doc.select(".wi_fic_desc").text()
        val genres = doc.select(".fic_genre").map { it.text() }

        return NovelDetails(
            title = title,
            url = novelUrl,
            coverUrl = cover,
            author = author,
            synopsis = summary,
            status = "Unknown",
            genres = genres,
            latestUpdate = ""
        )
    }

    override suspend fun getChapterList(novelUrl: String): List<Chapter> {
        // ScribbleHub loads chapters via ajax, but we can try to grab them if they are in the DOM
        // Realistically, for ScribbleHub you need to POST to an endpoint, but we will grab what's visible
        val doc = fetch(novelUrl)
        return doc.select(".toc_ol li a").mapIndexed { index, el ->
            Chapter(
                title = el.text(),
                url = el.attr("href"),
                novelUrl = novelUrl,
                index = index
            )
        }.reversed() // Usually descending order
    }

    override suspend fun getChapterContent(chapterUrl: String): String {
        val doc = fetch(chapterUrl)
        val content = doc.select("#chp_raw").html()
        return content.replace("<p>", "").replace("</p>", "\n\n")
            .replace("<br>", "\n")
            .replace(Regex("<[^>]*>"), "")
    }
}
