package com.example.wherebywebviewdemo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class CustomWebChromeClient extends WebChromeClient {
    private final Activity activity;

    public CustomWebChromeClient(Activity parentActivity) {
        super();
        activity = parentActivity;
    }

    @Override
    public void onPermissionRequest(final PermissionRequest request) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                request.grant(request.getResources());
            }
        });
    }
}
