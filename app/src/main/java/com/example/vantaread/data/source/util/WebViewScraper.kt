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
                var checkerScheduled = false
                
                override fun onReceivedError(
                    view: WebView,
                    request: android.webkit.WebResourceRequest,
                    error: android.webkit.WebResourceError
                ) {
                    super.onReceivedError(view, request, error)
                    if (finished) return
                    finished = true
                    if (continuation.isActive) continuation.resumeWith(Result.success(Jsoup.parse("<html><body>Error: ${error.description}</body></html>")))
                    view.destroy()
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (finished || checkerScheduled) return
                    checkerScheduled = true
                    
                    val checkHtml = object : Runnable {
                        override fun run() {
                            if (finished) return
                            attempts++
                            
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
                                
                                if (unescapedHtml.contains("challenge-error-text") || unescapedHtml.contains("Just a moment...") || unescapedHtml.length < 1000) {
                                    // Still on Cloudflare page or page hasn't fully rendered its content yet
                                    if (attempts > 12) {
                                        finished = true
                                        if (continuation.isActive) continuation.resume(Jsoup.parse(unescapedHtml))
                                        view.destroy()
                                    } else {
                                        // Check again in 400ms
                                        view.postDelayed(this, 400)
                                    }
                                } else {
                                    finished = true
                                    if (continuation.isActive) continuation.resume(Jsoup.parse(unescapedHtml))
                                    view.destroy()
                                }
                            }
                        }
                    }
                    
                    view.postDelayed(checkHtml, 200)
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
