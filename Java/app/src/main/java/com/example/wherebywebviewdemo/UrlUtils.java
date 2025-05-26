package com.example.wherebywebviewdemo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class UrlUtils {

    /**
     * Builds a full URL with query parameters from a base URL and a parameter map.
     * Keys and values are URL-encoded.
     */
    public static String buildUrlWithParams(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder builder = new StringBuilder(baseUrl);
        builder.append("?");

        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append("&");
            } else {
                first = false;
            }

            try {
                String encodedKey = URLEncoder.encode(entry.getKey(), "UTF-8");

                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    builder.append(encodedKey);
                } else {
                    String encodedValue = URLEncoder.encode(entry.getValue(), "UTF-8");
                    builder.append(encodedKey).append("=").append(encodedValue);
                }

            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("UTF-8 not supported", e);
            }
        }

        return builder.toString();
    }
}
