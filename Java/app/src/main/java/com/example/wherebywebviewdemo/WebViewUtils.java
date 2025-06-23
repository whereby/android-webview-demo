package com.example.wherebywebviewdemo;

import android.app.Activity;
import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class WebViewUtils {

    public static void configureWebView(
            WebView webView,
            Activity activity,
            @Nullable FileSaveHandler fileSaveHandler,
            @Nullable CustomWebChromeClient chromeClient
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

        if (chromeClient != null) {
            webView.setWebChromeClient(chromeClient);
        } else {
            webView.setWebChromeClient(new CustomWebChromeClient(activity));
        }

        if (fileSaveHandler != null) {
            webView.addJavascriptInterface(fileSaveHandler, "fileDownloadHandler");

            webView.setDownloadListener((url, userAgent, contentDisposition, mime, contentLength) -> {
                if (url.startsWith("blob:")) {
                    handleBlobDownload(webView, url, mime);
                } else {
                    Toast.makeText(activity, "Error: Url not supported for download.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private static void handleBlobDownload(WebView webView, String blobUrl, String mime) {
        webView.evaluateJavascript(
                "(async function() {" +
                        "const response = await fetch('" + blobUrl + "');" +
                        "const blob = await response.blob();" +
                        "const reader = new FileReader();" +
                        "reader.onload = function() {" +
                        "const payload = {" +
                        "data: reader.result," +
                        "mime: '" + mime + "'" +
                        "};" +
                        "window.fileDownloadHandler.handleBlobFromJs(JSON.stringify(payload));" +
                        "};" +
                        "reader.readAsDataURL(blob);" +
                        "})()",
                null
        );
    }
}

