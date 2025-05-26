package com.example.wherebywebviewdemo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class WebViewFragment extends Fragment {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private String roomUrlString;
    private WebView webView;
    private PermissionsManager permissionsManager;

    // ─────────────────────────────────────────────
    // Factory
    // ─────────────────────────────────────────────

    public static WebViewFragment newInstance(String roomUrlString) {
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(Constants.ROOM_URL_STRING, roomUrlString);
        fragment.setArguments(args);
        return fragment;
    }

    // ─────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_webview, container, false);

        if (getArguments() != null) {
            roomUrlString = getArguments().getString(Constants.ROOM_URL_STRING);
        }

        permissionsManager = new PermissionsManager(requireActivity());

        webView = view.findViewById(R.id.webview);

        WebViewUtils.configureWebView(
                webView,
                requireActivity(),
                new WebViewClient()
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();

        if (webView.getUrl() == null) {
            if (permissionsManager.isPendingPermissions()) {
                permissionsManager.requestPermissionsIfNeeded();
            } else {
                webView.loadUrl(roomUrlString);
            }
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
        if (!permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
