package com.example.wherebywebviewdemo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    WebView webView;

    private final static String TAG = WebViewFragment.class.getSimpleName();

    private String urlString;
    private PermissionsManager permissionsManager;
    private FileChooserHandler fileChooserHandler; // upload
    private FileDownloadHandler fileDownloadHandler; // download

    // ─────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────

    public static Intent newIntent(Context context, String roomUrlString) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(Constants.ROOM_URL_KEY, roomUrlString);
        return intent;
    }

    // ─────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────

    public WebViewActivity() {
        super(R.layout.activity_main);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get urlString
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

        urlString = bundle.getString(Constants.ROOM_URL_KEY);

        if (urlString == null || urlString.trim().isEmpty()) {
            Toast.makeText(this, "Invalid or missing room URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Download files
        ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    fileDownloadHandler.handleFileDownloadChooserResult(result.getResultCode(), result.getData());
                }
        );
        fileDownloadHandler = new FileDownloadHandler(this, createDocumentLauncher);

        // Upload files
        ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    fileChooserHandler.handleResult(result.getResultCode(), result.getData());
                }
        );
        fileChooserHandler = new FileChooserHandler(fileChooserLauncher);

        // Permissions manager
        ActivityResultLauncher<String[]> webViewPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    permissionsManager.handlePermissionsResult(result);
                }
        );
        permissionsManager = new PermissionsManager(this, webViewPermissionsLauncher);

        // Chrome client
        CustomWebChromeClient chromeClient = new CustomWebChromeClient(permissionsManager, fileChooserHandler);

        // Views
        setContentView(R.layout.activity_webview);
        webView = findViewById(R.id.webView);

        // Configure webView
        WebViewUtils.configureWebView(
                webView,
                chromeClient,
                fileDownloadHandler
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        if (webView == null) return;
        if (urlString == null) return;

        webView.onResume();

        if (webView.getUrl() == null) {
            webView.loadUrl(urlString);
        }
    }

    @Override
    public void onPause() {
        if (webView != null) {
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }
}
