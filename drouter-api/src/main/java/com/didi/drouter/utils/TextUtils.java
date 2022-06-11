package com.didi.drouter.utils;


import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;

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
        Map<String, String> paramMap = new ArrayMap<>();
        for (String key : rawUri.getQueryParameterNames()) {
            paramMap.put(key, rawUri.getQueryParameter(key));
        }
        return paramMap;
    }

    public static void appendExtra(Bundle bundle, Map<String, String> extra) {
        for (Map.Entry<String, String> entry : extra.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }
    }

}
