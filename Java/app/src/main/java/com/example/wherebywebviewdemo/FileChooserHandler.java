package com.example.wherebywebviewdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;

import androidx.activity.result.ActivityResultLauncher;

/**
 * FileChooserHandler manages the lifecycle of file selection for file input elements
 * in WebView (e.g., <input type="file">). It handles launching the Android file picker
 * and returning the selected URI(s) back to the WebView. Used to upload a file to share.
 */
class FileChooserHandler {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private ValueCallback<Uri[]> filePathCallback;
    private final ActivityResultLauncher<Intent> fileChooserLauncher;

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    /**
     * Constructs the FileChooserHandler with an ActivityResultLauncher used
     * to launch the file chooser intent.
     *
     * @param launcher ActivityResultLauncher used to handle file selection intent.
     */
    FileChooserHandler(ActivityResultLauncher<Intent> launcher) {
        this.fileChooserLauncher = launcher;
    }

    // ─────────────────────────────────────────────
    // File chooser lifecycle
    // ─────────────────────────────────────────────

    /**
     * Launches the file chooser and retains a callback to be notified when
     * the user has selected a file.
     *
     * @param intent            The intent provided by WebView to launch the file picker.
     * @param filePathCallback  The callback to pass selected file(s) back to WebView.
     * @return true if the launcher was available and intent was launched; false otherwise.
     */
    boolean launch(Intent intent, ValueCallback<Uri[]> filePathCallback) {
        if (this.filePathCallback != null) {
            // Clean up any previous callbacks
            this.filePathCallback.onReceiveValue(null);
        }

        this.filePathCallback = filePathCallback;

        if (fileChooserLauncher != null) {
            fileChooserLauncher.launch(intent);
            return true;
        }

        this.filePathCallback = null;
        return false;
    }

    /**
     * Should be called after file selection completes (e.g., from activity or fragment result).
     * Passes the result back to the WebView via the retained ValueCallback.
     *
     * @param resultCode Android activity result code (e.g., Activity.RESULT_OK).
     * @param data       The intent containing the selected file's URI.
     */
    void handleResult(int resultCode, Intent data) {
        if (filePathCallback == null) {
            return;
        }

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
