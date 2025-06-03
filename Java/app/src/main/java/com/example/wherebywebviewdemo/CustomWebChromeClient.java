package com.example.wherebywebviewdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;

public class CustomWebChromeClient extends WebChromeClient {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private final Activity activity;
    private ValueCallback<Uri[]> filePathCallback;
    private ActivityResultLauncher<Intent> uploadFileChooserLauncher;

    // ─────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────

    public CustomWebChromeClient(Activity parentActivity) {
        super();
        this.activity = parentActivity;
    }

    // ─────────────────────────────────────────────
    // Public Methods
    // ─────────────────────────────────────────────

    /**
     * Sets the launcher used to start the Android file chooser when uploading files.
     */
    public void setUploadFileChooserLauncher(ActivityResultLauncher<Intent> launcher) {
        this.uploadFileChooserLauncher = launcher;
    }

    /**
     * Grants permissions requested by the WebView (e.g. camera, microphone).
     */
    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        activity.runOnUiThread(() -> request.grant(request.getResources()));
    }

    /**
     * Triggered when the user interacts with an <input type="file"> element in a web page.
     * This method prepares and launches the Android file picker intent to allow the user to
     * select a file for upload.
     *
     * @return true if the launcher is available and the intent was successfully triggered;
     *         false otherwise, which cancels the file selection.
     */
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        if (this.filePathCallback != null) {
            this.filePathCallback.onReceiveValue(null);
        }
        this.filePathCallback = filePathCallback;

        Intent intent = fileChooserParams.createIntent();
        if (uploadFileChooserLauncher != null) {
            uploadFileChooserLauncher.launch(intent);
            return true;
        }

        this.filePathCallback = null;
        return false;
    }

    /**
     * Receives the result from the file picker and forwards it to the WebView.
     */
    public void handleFileChooserResult(int resultCode, Intent data) {
        if (filePathCallback == null) return;

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK && data != null) {
            String dataString = data.getDataString();
            if (dataString != null) {
                results = new Uri[]{Uri.parse(dataString)};
            }
        }

        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }
}
