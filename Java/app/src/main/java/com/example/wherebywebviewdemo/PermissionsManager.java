package com.example.wherebywebviewdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

public class PermissionsManager {

    // ─────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────

    private static final int PERMISSION_REQUEST_CODE = 1234;

    private static final String[] requiredPermissions = {
            Manifest.permission.CAMERA,                // Comment out if joining as a viewer
            Manifest.permission.MODIFY_AUDIO_SETTINGS, // Comment out if joining as a viewer
            Manifest.permission.RECORD_AUDIO           // Comment out if joining as a viewer
    };

    // ─────────────────────────────────────────────
    // Fields
    // ─────────────────────────────────────────────

    private final Activity activity;
    private boolean didShowPermissionAlertDialog = false; // Suppress repeat dialog

    // ─────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────

    public PermissionsManager(Activity parentActivity) {
        super();
        this.activity = parentActivity;
    }

    // ─────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────

    /**
     * Should be called when permissions may be needed (e.g. onResume or before accessing media).
     */
    protected void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                isPendingPermissions() &&
                !didShowPermissionAlertDialog) {
            requestPermissions();
        }
    }

    /**
     * Handles permission results. Returns true if the result was handled.
     */
    protected boolean onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResultsContainsDenials(grantResults)) {
                showPermissionDeniedDialog();
                didShowPermissionAlertDialog = true;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true if any required permissions are still pending.
     */
    protected boolean isPendingPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        return getPendingPermissions().length > 0;
    }

    // ─────────────────────────────────────────────
    // Internal Permission Logic
    // ─────────────────────────────────────────────

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestPermissions() {
        activity.requestPermissions(getPendingPermissions(), PERMISSION_REQUEST_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private String[] getPendingPermissions() {
        List<String> pendingPermissions = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                pendingPermissions.add(permission);
            }
        }
        return pendingPermissions.toArray(new String[0]);
    }

    private boolean grantResultsContainsDenials(int[] grantResults) {
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────
    // UI Feedback
    // ─────────────────────────────────────────────

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("Enable microphone and camera permissions in settings");
        builder.setCancelable(true);
        builder.setPositiveButton("ok", null);

        AlertDialog alert = builder.create();
        alert.show();
    }
}
