package com.didi.drouter.utils;


import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.util.ArrayMap;

import java.util.Collections;
import java.util.Map;

/**
 * Created by gaowei on 2018/11/29
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TextUtils {

    public static boolean isEmpty(@Nullable CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static @NonNull String getStandardRouterKey(Uri uri) {
        if (uri == null) return "@@$$";
        return getNonNull(uri.getScheme()) + "@@" +
                getNonNull(uri.getHost()) + "$$" +
                getNonNull(uri.getPath());
    }

    public static String getNonNull(String content) {
        return content == null ? "" : content;
    }

    // not \w or /
    public static boolean isRegex(String string) {
        return string != null && !string.matches("[\\w/]*");
    }

    public static String getPath(String pathWithQuery) {
        if (pathWithQuery != null) {
            int index = pathWithQuery.indexOf("?");
            return index != -1 ? pathWithQuery.substring(0, index) : pathWithQuery;
        }
        return null;
    }

    public static @NonNull Map<String, String> getQuery(Uri rawUri) {
        if (rawUri == null) {
            return Collections.emptyMap();
        }
        return getQuery(rawUri.toString());
    }

    private static @NonNull Map<String, String> getQuery(String rawUri) {
        if (rawUri == null) {
            return Collections.emptyMap();
        }

        int index = rawUri.indexOf("?");
        String query = index != -1 ? rawUri.substring(index + 1) : rawUri;
        index = query.indexOf("#");
        query = index != -1 ? query.substring(0, index) : query;

        Map<String, String> paramMap = new ArrayMap<>();
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            int end = (next == -1) ? query.length() : next;

            int separator = query.indexOf('=', start);
            if (separator > end || separator == -1) {
                separator = end;
            }

            String name = query.substring(start, separator);

            if (!android.text.TextUtils.isEmpty(name)) {
                String value = (separator == end ? "" : query.substring(separator + 1, end));
                paramMap.put(name, value);
            }

            // Move start to end of name.
            start = end + 1;
        } while (start < query.length());

        return Collections.unmodifiableMap(paramMap);
    }

    public static void appendExtra(Bundle bundle, Map<String, String> extra) {
        for (Map.Entry<String, String> entry : extra.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
    }

}
