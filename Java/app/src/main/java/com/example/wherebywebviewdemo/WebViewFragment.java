package com.example.wherebywebviewdemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebViewFragment extends Fragment {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private String roomUrlString;
    private WebView webView;

    private PermissionsManager permissionsManager;
    private CustomWebChromeClient chromeClient;
    private ActivityResultLauncher<Intent> fileDownloadPickerLauncher; // download
    private ActivityResultLauncher<Intent> fileUploadPickerLauncher; // upload
    private FileUploadHandler fileUploadHandler;
    private FileDownloadHandler fileDownloadHandler;

    // ─────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────

    public static WebViewFragment newInstance(String roomUrlString) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(Constants.ROOM_URL_KEY, roomUrlString);
        fragment.setArguments(args);
        return fragment;
    }

    // ─────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fileDownloadPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    fileDownloadHandler.handleFileDownloadPickerResult(result.getResultCode(), result.getData());
                }
        );
        fileDownloadHandler = new FileDownloadHandler(this.requireActivity(), fileDownloadPickerLauncher);

        fileUploadPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    this.chromeClient.handleFileChooserResult(result.getResultCode(), result.getData());
                }
        );
        fileUploadHandler = new FileUploadHandler(fileUploadPickerLauncher);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);
        webView = view.findViewById(R.id.webview);

        if (getArguments() != null) {
            roomUrlString = getArguments().getString(Constants.ROOM_URL_KEY);
        }

        permissionsManager = new PermissionsManager(this);
        chromeClient = new CustomWebChromeClient(permissionsManager, fileUploadHandler);

        WebViewUtils.configureWebView(
                webView,
                chromeClient,
                fileDownloadHandler
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();

        if (webView.getUrl() == null) {
            webView.loadUrl(roomUrlString);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
            webView = null;
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
