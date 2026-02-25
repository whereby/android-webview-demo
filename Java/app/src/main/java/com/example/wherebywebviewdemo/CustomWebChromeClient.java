package com.example.wherebywebviewdemo;

import android.content.Intent;
import android.net.Uri;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.Nullable;

/**
 * CustomWebChromeClient extends WebChromeClient to handle runtime permission
 * requests (e.g., camera, microphone) and file chooser interactions for file uploads.
 */
class CustomWebChromeClient extends WebChromeClient {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private final @Nullable PermissionsManager permissionsManager;
    private final @Nullable FileChooserHandler fileChooserHandler;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    /**
     * Constructs the ChromeClient with injected permission and file chooser handlers.
     *
     * @param permissionsManager Manages permission requests for camera/microphone.
     * @param fileChooserHandler  Manages file upload chooser interaction.
     */
    CustomWebChromeClient(@Nullable PermissionsManager permissionsManager, @Nullable FileChooserHandler fileChooserHandler) {
        this.permissionsManager = permissionsManager;
        this.fileChooserHandler = fileChooserHandler;
    }

    // ─────────────────────────────────────────────
    // WebView Permission API
    // ─────────────────────────────────────────────

    /**
     * Called when a web page requests access to protected resources like camera or mic.
     * Delegates permission handling to PermissionsManager.
     *
     * @param request The permission request from the WebView.
     */
    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        if (permissionsManager == null) {
            request.deny();
            return;
        }
        permissionsManager.checkAndRequestPermissionsForWebViewRequest(request);
    }

    // ─────────────────────────────────────────────
    // File Chooser Handling
    // ─────────────────────────────────────────────

    /**
     * Triggered by WebView when a file input element is clicked.
     * Delegates to the FileChooserHandler to launch file chooser and track result.
     *
     * @param webView           The WebView making the request.
     * @param filePathCallback  Callback to pass selected file(s) to WebView.
     * @param fileChooserParams WebView's file chooser intent config.
     * @return true if file picker was successfully launched; false to cancel.
     */
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        if (fileChooserHandler == null) {
            return false;
        }
        Intent intent = fileChooserParams.createIntent();
        return fileChooserHandler.launch(intent, filePathCallback);
    }
}
