package com.example.vantaread.data.source.util

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlin.coroutines.resume

object WebViewScraper {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun getHtml(context: Context, url: String): Document = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            var finished = false
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
            
            webView.webViewClient = object : WebViewClient() {
                var attempts = 0
                
                override fun onPageFinished(view: WebView, url: String) {
                    if (finished) return
                    attempts++
                    
                    val checkHtml = Runnable {
                        view.evaluateJavascript(
                            "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();"
                        ) { html ->
                            if (finished) return@evaluateJavascript
                            
                            val unescapedHtml = html?.drop(1)?.dropLast(1)
                                ?.replace("\\u003C", "<")
                                ?.replace("\\\"", "\"")
                                ?.replace("\\\\\"", "\\\"")
                                ?.replace("\\n", "\n")
                                ?.replace("\\r", "\r")
                                ?.replace("\\t", "\t") ?: ""
                            
                            if (unescapedHtml.contains("challenge-error-text") || unescapedHtml.contains("Just a moment...")) {
                                // Still on Cloudflare page
                                if (attempts > 5) {
                                    finished = true
                                    continuation.resume(Jsoup.parse(unescapedHtml))
                                    view.destroy()
                                }
                            } else {
                                finished = true
                                continuation.resume(Jsoup.parse(unescapedHtml))
                                view.destroy()
                            }
                        }
                    }
                    
                    // Cloudflare challenges take a few seconds
                    view.postDelayed(checkHtml, 3000)
                }
            }
            
            webView.loadUrl(url)
            
            continuation.invokeOnCancellation {
                if (!finished) {
                    finished = true
                    webView.destroy()
                }
            }
        }
    }
}
