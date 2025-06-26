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
public class FileDownloadHandler {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private static final String JS_INTERFACE_NAME = "fileDownloadHandler";

    private final Activity activity;
    private final ActivityResultLauncher<Intent> fileDownloadPickerLauncher;
    private byte[] base64DecodedFileBytes;

    public FileDownloadHandler(Activity activity, ActivityResultLauncher<Intent> launcher) {
        this.activity = activity;
        this.fileDownloadPickerLauncher = launcher;
    }

    /**
     * Attaches this FileDownloadHandler to the given WebView instance by:
     * - Registering a JavaScript interface so JavaScript can call back into
     *   the native layer to save files.
     * - Setting a DownloadListener that intercepts blob URL downloads
     *   and routes them through JavaScript to be handled natively.
     *
     * This setup is required to support blob-based file downloads triggered
     * from within a WebView (e.g., canvas recordings or file exports).
     */
    public void attachToWebView(WebView webView) {
        webView.addJavascriptInterface(this, JS_INTERFACE_NAME);

        webView.setDownloadListener((url, userAgent, contentDisposition, mime, contentLength) -> {
            if (url.startsWith("blob:")) {
                handleBlobDownload(webView, url, mime);
            } else {
                Toast.makeText(activity, "Error: Url not supported for download.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Injects JavaScript into the given WebView to fetch and decode a blob URL,
     * convert it into a Base64 string using a FileReader, and pass the resulting
     * payload back to the Android side via the fileDownloadHandler JavaScript interface.
     *
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
                    saveImageToGallery(fileName, fileData, mime);
                } else if (mime.startsWith("video/")) {
                    saveVideoToGallery(fileName, fileData, mime);
                } else {
                    File tempFile = new File(activity.getCacheDir(), UUID.randomUUID() + "_blobfile");
                    presentFilePickerAndSave(mime, fileName, tempFile, fileData);
                }
            } catch (Exception e) {
                Toast.makeText(activity, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────
    // Private Save Methods
    // ─────────────────────────────────────────────

    private void saveImageToGallery(String fileName, byte[] imageData, String mimeType) {
        String fileExtension = getFileExtensionFromMimeType(mimeType);
        String fullFileName = fileName + fileExtension;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fullFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/android-java-embedded-demo-app");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri uri = activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
                outputStream.write(imageData);
                outputStream.flush();
                Toast.makeText(activity, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(activity, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            File imageFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fullFileName);
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageData);
                fos.flush();
                Toast.makeText(activity, "Image saved successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(activity, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveVideoToGallery(String fileName, byte[] videoData, String mimeType) {
        String fileExtension = getFileExtensionFromMimeType(mimeType);
        String fullFileName = fileName + fileExtension;

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fullFileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/YourAppName");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Uri uri = activity.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri)) {
                outputStream.write(videoData);
                outputStream.flush();
                Toast.makeText(activity, "Video saved to gallery", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(activity, "Failed to save video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            File videoFile = new File(activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fullFileName);
            try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                fos.write(videoData);
                fos.flush();
                Toast.makeText(activity, "Video saved successfully" + videoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(activity, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void presentFilePickerAndSave(String mimeType, String suggestedFilename, File tempFile, byte[] base64Data) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, suggestedFilename);
        this.base64DecodedFileBytes = base64Data;
        fileDownloadPickerLauncher.launch(intent);
    }

    /**
     * Called after user has picked a file save location. This method writes
     * the prepared byte data to the selected Uri.
     */
    protected void handleFileDownloadPickerResult(int resultCode, Intent data) {
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
