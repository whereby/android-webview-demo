package com.example.wherebywebviewdemo;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private String roomUrlString;
    private WebView webView;
    private PermissionsManager permissionsManager;

    // ─────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────

    public WebViewActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        permissionsManager = new PermissionsManager(this);

        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "Missing intent", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Toast.makeText(this, "Missing parameters", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        roomUrlString = bundle.getString(Constants.ROOM_URL_STRING);

        if (roomUrlString == null || roomUrlString.trim().isEmpty()) {
            Toast.makeText(this, "Invalid or missing room URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        webView = findViewById(R.id.webView);

        WebViewUtils.configureWebView(
                webView,
                this,
                null
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();

        if (webView.getUrl() == null) {
            if (permissionsManager.isPendingPermissions()) {
                permissionsManager.requestPermissionsIfNeeded();
            } else {
                webView.loadUrl(roomUrlString);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
        }
    }

    // ─────────────────────────────────────────────
    // Permission Handling
    // ─────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (!permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
