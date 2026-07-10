import org.jsoup.Jsoup
import java.io.File

fun main() {
    try {
        val url = "https://wtr-lab.com/en/novel-list?search=shadow"
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .timeout(10000)
            .get()
        
        File("search_output.html").writeText(doc.html())
        println("Done!")
    } catch (e: Exception) {
        println(e)
    }
}
