package com.didi.drouter.demo.web

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.didi.drouter.annotation.Router
import com.didi.drouter.demo.R

@Router(path = "/activity/webview")
class WebActivity : AppCompatActivity() {
    private lateinit var webview: WebView

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        webview = findViewById(R.id.webview)
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.url?.also {
                        if (it.scheme?.startsWith("didi://") == true) {
                            // intercept didi://
                            startActivity(Intent(Intent.ACTION_VIEW, it))
                            return true
                        }
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("didi://")) {
                    // intercept didi://
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }
        }
        intent.getStringExtra("url")?.also {
            webview.loadUrl(it)
        }
    }
}
