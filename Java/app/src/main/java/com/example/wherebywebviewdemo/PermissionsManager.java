package com.example.wherebywebviewdemo;

import android.Manifest;
import android.app.Activity;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import android.os.Build;
import android.webkit.PermissionRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages runtime permission requests for WebView-initiated resource access
 * (e.g., camera, microphone), including one-time denial tracking for better UX.
 */
public class PermissionsManager {

    private static final int WEBVIEW_PERMISSION_REQUEST_CODE = 5678;

    private final @Nullable Activity activity;
    private final @Nullable Fragment fragment;
    private PermissionRequest pendingWebViewRequest;

    // Session-scope flags to avoid repeatedly prompting after denial
    private boolean hasDeniedCameraPermission = false;
    private boolean hasDeniedMicrophonePermission = false;

    public PermissionsManager(Activity activity) {
        this.activity = activity;
        this.fragment = null;
    }

    public PermissionsManager(Fragment fragment) {
        this.activity = null;
        this.fragment = fragment;
    }

    /**
     * Handles a WebView PermissionRequest by checking whether the requested
     * resources (camera/mic) are allowed, denied, or need to be requested.
     *
     * If permissions are granted, it directly grants them to the WebView.
     * If permissions are missing, it triggers a runtime request.
     * If permissions were already denied in this session, it auto-denies the request.
     *
     * @param request The WebView PermissionRequest to handle.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkAndRequestPermissionsForWebViewRequest(PermissionRequest request) {
        boolean cameraNeeded = false;
        boolean micNeeded = false;

        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                cameraNeeded = true;
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                micNeeded = true;
            }
        }

        boolean hasCamera = !cameraNeeded || isPermissionGranted(Manifest.permission.CAMERA);
        boolean hasMic = !micNeeded || isPermissionGranted(Manifest.permission.RECORD_AUDIO);

        if ((cameraNeeded && hasDeniedCameraPermission) || (micNeeded && hasDeniedMicrophonePermission)) {
            request.deny();
            return;
        }

        if (hasCamera && hasMic) {
            request.grant(request.getResources());
        } else {
            pendingWebViewRequest = request;
            List<String> permissionsToRequestList = new ArrayList<>();
            if (cameraNeeded && !hasCamera) permissionsToRequestList.add(Manifest.permission.CAMERA);
            if (micNeeded && !hasMic) permissionsToRequestList.add(Manifest.permission.RECORD_AUDIO);

            String[] permissionsToRequestArray = permissionsToRequestList.toArray(new String[0]);
            if (fragment != null) {
                fragment.requestPermissions(permissionsToRequestArray, WEBVIEW_PERMISSION_REQUEST_CODE);
            } else if (activity != null) {
                activity.requestPermissions(permissionsToRequestArray, WEBVIEW_PERMISSION_REQUEST_CODE);
            } else {
                throw new IllegalStateException("PermissionsManager requires an activity or fragment");
            }
        }
    }

    /**
     * Handles the result of the Android runtime permission dialog for WebView requests.
     * Grants or denies the WebView PermissionRequest accordingly, and flags denials
     * to suppress repeated prompts in the same session.
     *
     * @param permissions   Array of permissions requested.
     * @param grantResults  Corresponding grant results.
     */
    public void handleWebViewPermissionResult(String[] permissions, int[] grantResults) {
        if (pendingWebViewRequest == null) return;

        boolean allGranted = true;

        for (int i = 0; i < permissions.length; i++) {
            String perm = permissions[i];
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;

            if (!granted) {
                if (Manifest.permission.CAMERA.equals(perm)) {
                    hasDeniedCameraPermission = true;
                } else if (Manifest.permission.RECORD_AUDIO.equals(perm)) {
                    hasDeniedMicrophonePermission = true;
                }
                allGranted = false;
            }
        }

        if (allGranted) {
            pendingWebViewRequest.grant(pendingWebViewRequest.getResources());
        } else {
            pendingWebViewRequest.deny();
        }

        pendingWebViewRequest = null;
    }

    /**
     * Utility method to check if a given permission is already granted.
     *
     * @param permission Android manifest permission string.
     * @return true if the permission is granted; false otherwise.
     */
    private boolean isPermissionGranted(String permission) {
        Context context = fragment != null ? fragment.requireContext() : activity;
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Entry point from the Activity's onRequestPermissionsResult().
     * Delegates to WebView-specific permission handling if the request code matches.
     *
     * @param requestCode  Request code from onRequestPermissionsResult.
     * @param permissions  Array of requested permissions.
     * @param grantResults Grant results corresponding to the permissions.
     * @return true if the request was handled here; false otherwise.
     */
    public boolean handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == WEBVIEW_PERMISSION_REQUEST_CODE) {
            handleWebViewPermissionResult(permissions, grantResults);
            return true;
        }
        return false;
    }
}
