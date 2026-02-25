package com.example.wherebywebviewdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

@SuppressLint("ValidFragment")
public class WebViewFragment extends Fragment {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    View view;
    WebView webView;

    private final static String TAG = WebViewFragment.class.getSimpleName();

    private String urlString;
    private PermissionsManager permissionsManager;
    private FileChooserHandler fileChooserHandler; // upload
    private FileDownloadHandler fileDownloadHandler; // download

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
    // Fragment lifecycle
    // ─────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get urlString
        if (getArguments() != null) {
            urlString = getArguments().getString(Constants.ROOM_URL_KEY);
        }

        // Download files
        ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    fileDownloadHandler.handleFileDownloadChooserResult(result.getResultCode(), result.getData());
                }
        );
        fileDownloadHandler = new FileDownloadHandler(this.requireActivity(), createDocumentLauncher);

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_webview, container, false);
        webView = view.findViewById(R.id.webView);

        CustomWebChromeClient chromeClient = new CustomWebChromeClient(permissionsManager, fileChooserHandler);

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
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
    }

}
