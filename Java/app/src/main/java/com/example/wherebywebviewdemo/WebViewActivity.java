package com.example.wherebywebviewdemo;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class WebViewActivity extends AppCompatActivity {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private String roomUrlString;
    private WebView webView;

    private PermissionsManager permissionsManager;
    private CustomWebChromeClient chromeClient;
    private ActivityResultLauncher<Intent> fileDownloadPickerLauncher;
    private ActivityResultLauncher<Intent> fileUploadPickerLauncher;
    private FileUploadHandler fileUploadHandler;
    private FileDownloadHandler fileDownloadHandler;

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

        roomUrlString = bundle.getString(Constants.ROOM_URL_KEY);

        if (roomUrlString == null || roomUrlString.trim().isEmpty()) {
            Toast.makeText(this, "Invalid or missing room URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        permissionsManager = new PermissionsManager(this);

        webView = findViewById(R.id.webView);

        // Download: Register launcher for saving downloaded files
        fileDownloadPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    fileDownloadHandler.handleFileDownloadPickerResult(result.getResultCode(), result.getData());
                }
        );

        fileDownloadHandler = new FileDownloadHandler(this, fileDownloadPickerLauncher);

        // Upload: Register launcher for file chooser
        fileUploadPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        this.chromeClient.handleFileChooserResult(result.getResultCode(), result.getData());
                    }
                }
        );

        fileUploadHandler = new FileUploadHandler(fileUploadPickerLauncher);

        chromeClient = new CustomWebChromeClient(permissionsManager, fileUploadHandler);

        WebViewUtils.configureWebView(
                webView,
                chromeClient,
                fileDownloadHandler
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();

        if (webView.getUrl() == null) {
            webView.loadUrl(roomUrlString);
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

        if (!permissionsManager.handleRequestPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
