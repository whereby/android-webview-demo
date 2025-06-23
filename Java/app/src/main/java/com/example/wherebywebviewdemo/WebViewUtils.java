package com.example.wherebywebviewdemo;

import android.app.Activity;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewUtils {

    public static void configureWebView(
            WebView webView,
            CustomWebChromeClient chromeClient,
            FileDownloadHandler fileDownloadHandler
    ) {
        // ─────────────────────────────────────────────
        // Web settings
        // ─────────────────────────────────────────────

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Optional restrictions
        // settings.setAllowFileAccess(false);
        // settings.setAllowContentAccess(false);

        // ─────────────────────────────────────────────
        // Cookie settings
        // ─────────────────────────────────────────────

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // ─────────────────────────────────────────────
        // WebView clients
        // ─────────────────────────────────────────────

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Prevent navigation inside the WebView.
                // Feel free to add a list of allowed url, such as Whereby policy.
                // If not set, shared files will open in a new page, resulting in the user leaving the meeting.
                return true;
            }
        });

        webView.setWebChromeClient(chromeClient);

        fileDownloadHandler.attachToWebView(webView);
    }
}

