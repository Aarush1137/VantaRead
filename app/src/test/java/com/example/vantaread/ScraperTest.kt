package com.example.vantaread

import org.jsoup.Jsoup
import org.junit.Test
import java.net.URLEncoder

class ScraperTest {
    @Test
    fun testRoyalRoad() {
        println("--- ROYAL ROAD ---")
        try {
            val searchUrl = "https://www.royalroad.com/fictions/search?title=solo"
            val doc = Jsoup.connect(searchUrl).userAgent("Mozilla/5.0").get()
            val items = doc.select(".fiction-list-item")
            println("Search results for 'solo': ${items.size}")
            var novelUrl = ""
            for (item in items.take(1)) {
                val titleElement = item.selectFirst(".fiction-title a")
                println("Title: ${titleElement?.text()}")
                novelUrl = "https://www.royalroad.com" + titleElement?.attr("href")
                println("URL: $novelUrl")
            }

            if (novelUrl.isNotEmpty()) {
                val detailDoc = Jsoup.connect(novelUrl).userAgent("Mozilla/5.0").get()
                val chapterElements = detailDoc.select("#chapters tbody tr")
                println("Chapters found: ${chapterElements.size}")
                for (tr in chapterElements.take(2)) {
                    val a = tr.selectFirst("a[href]")
                    println("Chapter: ${a?.text()} - ${a?.attr("href")}")
                }
            }
        } catch (e: Exception) {
            println("RoyalRoad Error: ${e.message}")
        }
    }

    @Test
    fun testNovelFull() {
        println("--- NOVEL FULL ---")
        try {
            val searchUrl = "https://novelfull.com/search?keyword=solo"
            val doc = Jsoup.connect(searchUrl).userAgent("Mozilla/5.0").get()
            val items = doc.select(".list-truyen .row")
            println("Search results for 'solo': ${items.size}")
            var novelUrl = ""
            for (item in items.take(1)) {
                val titleElement = item.selectFirst("h3.truyen-title a")
                println("Title: ${titleElement?.text()}")
                novelUrl = "https://novelfull.com" + titleElement?.attr("href")
                println("URL: $novelUrl")
            }

            if (novelUrl.isNotEmpty()) {
                val detailDoc = Jsoup.connect(novelUrl).userAgent("Mozilla/5.0").get()
                val chapterElements = detailDoc.select("ul.list-chapter li a")
                println("Chapters found: ${chapterElements.size}")
                for (a in chapterElements.take(2)) {
                    println("Chapter: ${a.text()} - ${a.attr("href")}")
                }
            }
            
            val popUrl = "https://novelfull.com/most-popular"
            val popDoc = Jsoup.connect(popUrl).userAgent("Mozilla/5.0").get()
            val popItems = popDoc.select(".list-truyen .row")
            println("Popular novels found: ${popItems.size}")
        } catch (e: Exception) {
            println("NovelFull Error: ${e.message}")
        }
    }
}
