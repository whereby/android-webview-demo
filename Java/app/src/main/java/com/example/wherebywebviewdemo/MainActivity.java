package com.example.wherebywebviewdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ─────────────────────────────────────────────
    // Constants
    // ─────────────────────────────────────────────

    private final String INITIAL_ROOM_URL_STRING = "https://yourWherebyRoomUrl";
    private final Map<String, String> INITIAL_ROOM_URL_PARAMS = Map.of(
            "needancestor", "",
            "skipMediaPermissionPrompt", ""
    );

    // ─────────────────────────────────────────────
    // Views
    // ─────────────────────────────────────────────

    private Button activityButton;
    private Button fragmentButton;
    private TextInputEditText textInput;

    // ─────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────

    public MainActivity() {
        super(R.layout.activity_main);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textInput = findViewById(R.id.textInput);
        activityButton = findViewById(R.id.activityButton);
        fragmentButton = findViewById(R.id.fragmentButton);

        String fullUrl = UrlUtils.buildUrlWithParams(INITIAL_ROOM_URL_STRING, INITIAL_ROOM_URL_PARAMS);
        textInput.setText(fullUrl);

        activityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = getValidatedRoomUrl();
                if (url != null) {
                    launchWebViewActivity(url);
                }
            }
        });

        fragmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = getValidatedRoomUrl();
                if (url != null) {
                    loadWebViewFragment(url);
                }
            }
        });
    }

    // ─────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────

    private void launchWebViewActivity(String roomUrlString) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(Constants.ROOM_URL_KEY, roomUrlString);
        startActivity(intent);
    }

    private void loadWebViewFragment(String roomUrlString) {
        WebViewFragment fragment = WebViewFragment.newInstance(roomUrlString);

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private @Nullable String getValidatedRoomUrl() {
        Editable editable = textInput.getText();
        String url = (editable != null) ? editable.toString().trim() : "";

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a room URL", Toast.LENGTH_SHORT).show();
            return null;
        }

        if (!Patterns.WEB_URL.matcher(url).matches()) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return null;
        }

        return url;
    }
}