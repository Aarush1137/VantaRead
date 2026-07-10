import org.jsoup.Jsoup

fun main() {
    val url = "https://wtr-lab.com/en/novel/shadow-slave"
    val doc = Jsoup.connect(url)
        .userAgent("Mozilla/5.0")
        .followRedirects(true)
        .get()
    
    println("Title: " + doc.selectFirst("h1")?.text())
    println("Chapter elements:")
    doc.select("a").forEach {
        val href = it.attr("href")
        if (href.contains("/chapter-")) {
            println(href + " -> " + it.text())
        }
    }
}
