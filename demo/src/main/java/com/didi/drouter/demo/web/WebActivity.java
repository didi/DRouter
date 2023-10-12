package com.didi.drouter.demo.web;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.demo.R;

@Router(path = "/activity/webview")
public class WebActivity extends Activity {

    WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webview = findViewById(R.id.webview);
        webview.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.startsWith("didi://")) {
                    // intercept didi://
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
        });

        webview.loadUrl(getIntent().getStringExtra("url"));
    }


}
