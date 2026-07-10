import org.jsoup.Jsoup
import java.io.File

fun main() {
    try {
        val url = "https://novelfull.com/search?keyword=shadow"
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(10000)
            .get()
        
        File("novelfull_search.html").writeText(doc.html())
        println("Done!")
    } catch (e: Exception) {
        println(e)
    }
}
