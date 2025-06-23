package com.example.wherebywebviewdemo;

import android.content.Intent;
import android.os.Build;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;

/**
 * CustomWebChromeClient extends WebChromeClient to handle runtime permission
 * requests (e.g., camera, microphone) and file chooser interactions for file uploads.
 */
public class CustomWebChromeClient extends WebChromeClient {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private final PermissionsManager permissionsManager;
    private final FileUploadHandler fileUploadHandler;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    /**
     * Constructs the ChromeClient with injected permission and upload file handlers.
     *
     * @param permissionsManager Manages permission requests for camera/microphone.
     * @param fileUploadHandler  Manages file upload chooser interaction.
     */
    public CustomWebChromeClient(PermissionsManager permissionsManager, FileUploadHandler fileUploadHandler) {
        this.permissionsManager = permissionsManager;
        this.fileUploadHandler = fileUploadHandler;
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
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        permissionsManager.checkAndRequestPermissionsForWebViewRequest(request);
    }

    // ─────────────────────────────────────────────
    // File Upload Handling
    // ─────────────────────────────────────────────

    /**
     * Triggered by WebView when a file input element is clicked.
     * Delegates to the FileUploadHandler to launch file chooser and track result.
     *
     * @param webView           The WebView making the request.
     * @param filePathCallback  Callback to pass selected file(s) to WebView.
     * @param fileChooserParams WebView's file chooser intent config.
     * @return true if file picker was successfully launched; false to cancel.
     */
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<android.net.Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        Intent intent = fileChooserParams.createIntent();
        return fileUploadHandler.showFileChooser(intent, filePathCallback);
    }

    /**
     * Should be called from the activity/fragment when file chooser result is received.
     *
     * @param resultCode The result code returned by the file picker.
     * @param data       The intent containing selected file data.
     */
    public void handleFileChooserResult(int resultCode, Intent data) {
        fileUploadHandler.handleFileChooserResult(resultCode, data);
    }
}
