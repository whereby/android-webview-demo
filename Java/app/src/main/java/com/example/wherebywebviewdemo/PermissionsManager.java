package com.example.wherebywebviewdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.webkit.PermissionRequest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages runtime permission requests for WebView-initiated resource access
 * (e.g., camera, microphone), including one-time denial tracking for better UX.
 */
class PermissionsManager {

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private final @Nullable Activity activity;
    private final @Nullable Fragment fragment;

    private PermissionRequest pendingWebViewRequest;
    @Nullable private String[] pendingAndroidPermissions;
    @NonNull private final ActivityResultLauncher<String[]> permissionsLauncher;


    // ─────────────────────────────────────────────
    // State
    // ─────────────────────────────────────────────

    // Session-scope flags to avoid repeatedly prompting after denial
    private boolean hasDeniedCameraPermission = false;
    private boolean hasDeniedMicrophonePermission = false;

    // ─────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────

    PermissionsManager(@NonNull Activity activity, @NonNull ActivityResultLauncher<String[]> permissionsLauncher) {
        this.activity = activity;
        this.fragment = null;
        this.permissionsLauncher = permissionsLauncher;
    }

    PermissionsManager(@NonNull Fragment fragment, @NonNull ActivityResultLauncher<String[]> permissionsLauncher) {
        this.activity = null;
        this.fragment = fragment;
        this.permissionsLauncher = permissionsLauncher;
    }

    // ─────────────────────────────────────────────
    // API
    // ─────────────────────────────────────────────

    /**
     * Handles a WebView {@link PermissionRequest} by determining whether camera and/or microphone
     * permissions are required and currently granted.
     *
     * <p>If all required Android runtime permissions are already granted, this method immediately
     * grants the WebView request via {@link PermissionRequest#grant(String[])}.
     *
     * <p>If one or more required permissions are missing, it launches an Android runtime permission
     * request and stores the {@code PermissionRequest} as pending. The WebView request will be
     * granted or denied asynchronously when the permission result is received (see
     * {@code handlePermissionsResult(...)}).
     *
     * <p>If the user has already denied the required permission(s) during this session, the WebView
     * request is denied immediately to avoid repeated prompts.
     *
     * @param request The WebView permission request to handle.
     * @throws IllegalStateException if no permissions launcher has been configured.
     */
    void checkAndRequestPermissionsForWebViewRequest(@NonNull PermissionRequest request) {
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
            return;
        }

        // Need to request at least one permission
        pendingWebViewRequest = request;

        List<String> permissionsToRequestList = new ArrayList<>();
        if (cameraNeeded && !hasCamera) {
            permissionsToRequestList.add(Manifest.permission.CAMERA);
        }
        if (micNeeded && !hasMic) {
            permissionsToRequestList.add(Manifest.permission.RECORD_AUDIO);
        }

        String[] permissionsToRequestArray = permissionsToRequestList.toArray(new String[0]);
        pendingAndroidPermissions = permissionsToRequestArray;

        permissionsLauncher.launch(permissionsToRequestArray);
    }

    // ─────────────────────────────────────────────
    // Permission Results
    // ─────────────────────────────────────────────

    /**
     * Receives the result of an Android runtime permission request initiated via
     * {@link ActivityResultLauncher}.
     *
     * <p>This method is invoked from the permission launcher callback and delegates
     * the result to WebView-specific permission handling. If there is a pending
     * {@link PermissionRequest}, it will be granted or denied
     * asynchronously based on the provided permission results.
     *
     * <p>This method assumes that the permission result corresponds to the most
     * recent WebView permission request.
     *
     * @param result A map of permission names to grant state, as returned by
     * {@link androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions}.
     */
    void handlePermissionsResult(@NonNull Map<String, Boolean> result) {
        if (pendingWebViewRequest == null) {
            return;
        }

        boolean allGranted = true;

        if (pendingAndroidPermissions != null) {
            for (String perm : pendingAndroidPermissions) {
                boolean granted = Boolean.TRUE.equals(result.get(perm));
                if (!granted) {
                    if (Manifest.permission.CAMERA.equals(perm)) {
                        hasDeniedCameraPermission = true;
                    }
                    if (Manifest.permission.RECORD_AUDIO.equals(perm)) {
                        hasDeniedMicrophonePermission = true;
                    }
                    allGranted = false;
                }
            }
        } else {
            // fallback: deny if we don't know what was requested
            allGranted = false;
        }

        if (allGranted) {
            pendingWebViewRequest.grant(pendingWebViewRequest.getResources());
        }
        else {
            pendingWebViewRequest.deny();
        }

        pendingWebViewRequest = null;
        pendingAndroidPermissions = null;
    }

    // ─────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────

    /**
     * Returns a valid Context from the hosting component.
     *
     * @return a non-null Context
     * @throws IllegalStateException if neither an activity nor a fragment is available
     */
    private Context requireContext() {
        if (fragment != null) {
            return fragment.requireContext();
        }
        if (activity != null) {
            return activity;
        }
        throw new IllegalStateException("PermissionsManager requires an activity or fragment");
    }

    /**
     * Utility method to check if a given permission is already granted.
     *
     * @param permission Android manifest permission string.
     * @return true if the permission is granted; false otherwise.
     */
    private boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                permission
        ) == PackageManager.PERMISSION_GRANTED;
    }
}
