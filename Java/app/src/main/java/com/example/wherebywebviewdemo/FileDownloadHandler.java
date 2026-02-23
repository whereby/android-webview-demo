package com.example.wherebywebviewdemo;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * FileDownloadHandler is responsible for decoding Base64 blobs received from JavaScript,
 * and saving them to the device either via the media store (for images/videos) or
 * by prompting the user with a file picker (for generic files).
 */
class FileDownloadHandler {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private static final String JS_INTERFACE_NAME = "fileDownloadHandler";

    private final Activity activity;
    private final ActivityResultLauncher<Intent> createDocumentLauncher;
    private byte[] base64DecodedFileBytes;

    // ─────────────────────────────────────────────
    // Types
    // ─────────────────────────────────────────────

    private enum MediaKind {
        IMAGE,
        VIDEO
    }

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    FileDownloadHandler(Activity activity, ActivityResultLauncher<Intent> launcher) {
        this.activity = activity;
        this.createDocumentLauncher = launcher;
    }

    // ─────────────────────────────────────────────
    // WebView integration
    // ─────────────────────────────────────────────

    /**
     * Attaches this FileDownloadHandler to the given WebView instance by:
     * - Registering a JavaScript interface so JavaScript can call back into
     *   the native layer to save files.
     * - Setting a DownloadListener that intercepts blob URL downloads
     *   and routes them through JavaScript to be handled natively.
     * <p>
     * This setup is required to support blob-based file downloads triggered
     * from within a WebView (e.g., canvas recordings or file exports).
     */
    void attachToWebView(WebView webView) {
        webView.addJavascriptInterface(this, JS_INTERFACE_NAME);

        webView.setDownloadListener((url, userAgent, contentDisposition, mime, contentLength) -> {
            if (url.startsWith("blob:")) {
                handleBlobDownload(webView, url, mime);
            } else {
                Toast.makeText(activity, "Error: Url not supported for download.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────
    // JavaScript bridge
    // ─────────────────────────────────────────────

    /**
     * Called from JavaScript to initiate saving a blob to local storage.
     * Differentiates between media types and triggers appropriate save logic.
     */
    @JavascriptInterface
    public void handleBlobFromJs(String jsonPayload) {
        activity.runOnUiThread(() -> {
            try {
                JSONObject json = new JSONObject(jsonPayload);
                String mime = json.optString("mime", "application/octet-stream");
                String base64Data = json.getString("data");

                String parsedBase64Data = base64Data.startsWith("data:") ? base64Data.split(",")[1] : base64Data;
                byte[] fileData = Base64.decode(parsedBase64Data, Base64.DEFAULT);
                String fileName = "file_" + UUID.randomUUID();

                if (mime.startsWith("image/")) {
                    saveMedia(MediaKind.IMAGE, fileName, fileData, mime);
                } else if (mime.startsWith("video/")) {
                    saveMedia(MediaKind.VIDEO, fileName, fileData, mime);
                } else {
                    launchSaveFileChooser(mime, fileName, fileData);
                }
            } catch (Exception e) {
                Toast.makeText(activity, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────
    // Blob extraction (JavaScript injection)
    // ─────────────────────────────────────────────

    /**
     * Injects JavaScript into the given WebView to fetch and decode a blob URL,
     * convert it into a Base64 string using a FileReader, and pass the resulting
     * payload back to the Android side via the fileDownloadHandler JavaScript interface.
     * <p>
     * This is necessary because WebView's native DownloadListener cannot handle blob: URLs
     * directly—JavaScript must be used to access the blob content.
     */
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
                        "window." + JS_INTERFACE_NAME + ".handleBlobFromJs(JSON.stringify(payload));" +
                        "};" +
                        "reader.readAsDataURL(blob);" +
                        "})()",
                null
        );
    }

    // ─────────────────────────────────────────────
    // Save flows
    // ─────────────────────────────────────────────

    private void saveMedia(
            MediaKind kind,
            String fileName,
            byte[] data,
            String mimeType
    ) {
        String fileExtension = getFileExtensionFromMimeType(mimeType);
        String fullFileName = fileName + fileExtension;

        final Uri collection;
        final String legacyDirectory;

        switch (kind) {
            case IMAGE:
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                legacyDirectory = Environment.DIRECTORY_PICTURES;
                break;

            case VIDEO:
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                legacyDirectory = Environment.DIRECTORY_MOVIES;
                break;

            default:
                Toast.makeText(activity, "Unable to save file (unsupported type)", Toast.LENGTH_SHORT).show();
                return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

            Uri uri = activity.getContentResolver().insert(collection, values);
            if (uri == null) {
                Toast.makeText(activity, "Unable to save file (storage unavailable)", Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    Toast.makeText(activity, "Unable to save file (cannot open output)", Toast.LENGTH_SHORT).show();
                    return;
                }

                outputStream.write(data);
                outputStream.flush();
                Toast.makeText(activity, "Saved to device storage", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                Toast.makeText(activity, "Unable to save file (write failed)", Toast.LENGTH_SHORT).show();
            }

        } else {
            File outFile = new File(activity.getExternalFilesDir(legacyDirectory), fullFileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(data);
                fos.flush();
                Toast.makeText(activity, "Saved to device storage", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(activity, "Unable to save file (write failed)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchSaveFileChooser(String mimeType, String suggestedFilename, byte[] base64Data) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedFilename);
        this.base64DecodedFileBytes = base64Data;
        createDocumentLauncher.launch(intent);
    }

    // ─────────────────────────────────────────────
    // Activity result handling
    // ─────────────────────────────────────────────

    /**
     * Called after user has picked a file save location. This method writes
     * the prepared byte data to the selected Uri.
     */
    void handleFileDownloadChooserResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    OutputStream outputStream = activity.getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        outputStream.write(base64DecodedFileBytes);
                        outputStream.close();
                        Toast.makeText(activity, "File saved successfully", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(activity, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────

    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return "";

        switch (mimeType) {
            case "image/jpeg": return ".jpg";
            case "image/png": return ".png";
            case "image/webp": return ".webp";
            case "video/mp4": return ".mp4";
            case "video/mpeg": return ".mpeg";
            case "video/webm": return ".webm";
            case "image/gif": return ".gif";
            default: return "";
        }
    }
}
