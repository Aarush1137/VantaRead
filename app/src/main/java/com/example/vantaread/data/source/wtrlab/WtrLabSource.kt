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
        val doc = Jsoup.connect(url).get()
        
        doc.select(".search-item").map { element ->
            val aElement = element.selectFirst("a")
            val imgElement = element.selectFirst("img")
            
            Novel(
                url = baseUrl + aElement?.attr("href"),
                title = aElement?.text() ?: "Unknown",
                coverUrl = imgElement?.attr("src") ?: ""
            )
        }
    }

    override suspend fun getPopularNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/en"
        val doc = Jsoup.connect(url).get()
        
        doc.select(".popular-item").map { element ->
            val aElement = element.selectFirst("a")
            val imgElement = element.selectFirst("img")
            
            Novel(
                url = baseUrl + aElement?.attr("href"),
                title = aElement?.text() ?: "Unknown",
                coverUrl = imgElement?.attr("src") ?: ""
            )
        }
    }

    override suspend fun getNovelDetails(novelUrl: String): NovelDetails = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(novelUrl).get()
        
        val title = doc.selectFirst("h1")?.text() ?: "Unknown Title"
        val coverUrl = doc.selectFirst(".novel-cover img")?.attr("src") ?: ""
        val synopsis = doc.selectFirst(".description")?.text() ?: "No synopsis available."
        
        var author = "Unknown"
        var status = "Unknown"
        val genres = mutableListOf<String>()
        
        doc.select(".novel-info div").forEach { infoDiv ->
            val text = infoDiv.text()
            if (text.startsWith("Author:")) author = text.substringAfter("Author:").trim()
            if (text.startsWith("Status:")) status = text.substringAfter("Status:").trim()
        }
        
        doc.select(".genres a").forEach { genreElement ->
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
        // Example: https://wtr-lab.com/en/novel/53992/lord-god-tier-attribute-recruits-fallen-angels-of-original-sin
        // We might need to fetch chapter list differently, usually there's a chapter list page or it's on the main page.
        // For now, assuming it's on the novel page in a list
        val doc = Jsoup.connect(novelUrl).get()
        val chapters = mutableListOf<Chapter>()
        
        // Find links that look like chapters
        doc.select("a[href*=/chapter-]").forEachIndexed { index, element ->
            chapters.add(
                Chapter(
                    url = if (element.attr("href").startsWith("http")) element.attr("href") else baseUrl + element.attr("href"),
                    novelUrl = novelUrl,
                    title = element.text(),
                    index = index
                )
            )
        }
        
        if (chapters.isEmpty()) {
            // Some sites load chapters via JS or on a separate page.
            // A basic fallback if we can't find chapter links
            doc.select(".chapter-list a, .list-chapter a, ul.chapter-list li a").forEachIndexed { index, element ->
                 chapters.add(
                    Chapter(
                        url = if (element.attr("href").startsWith("http")) element.attr("href") else baseUrl + element.attr("href"),
                        novelUrl = novelUrl,
                        title = element.text(),
                        index = index
                    )
                )
            }
        }
        
        chapters
    }

    override suspend fun getChapterContent(chapterUrl: String): String = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(chapterUrl).get()
        // WTR-Lab chapter content is usually inside an article or specific div
        val contentElement = doc.selectFirst(".chapter-content, #chapter-content, article")
        contentElement?.html() ?: "Failed to extract chapter content."
    }
}
