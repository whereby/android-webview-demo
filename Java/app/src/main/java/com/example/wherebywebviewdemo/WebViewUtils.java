package com.example.wherebywebviewdemo;

import android.app.Activity;
import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

public class WebViewUtils {

    public static void configureWebView(
            WebView webView,
            Context context,
            @Nullable WebViewClient client
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

        if (context instanceof Activity) {
            webView.setWebChromeClient(new CustomWebChromeClient((Activity) context));
        } else {
            throw new IllegalArgumentException("Context must be an Activity for permission handling");
        }

        if (client != null) {
            webView.setWebViewClient(client);
        } else {
            webView.setWebViewClient(new WebViewClient());
        }
    }
}

