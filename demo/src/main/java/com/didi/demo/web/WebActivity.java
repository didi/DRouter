package com.didi.demo.web;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

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

//        webview.setWebViewClient(new WebViewClient());
        webview.loadUrl(getIntent().getStringExtra("url"));
    }


}
