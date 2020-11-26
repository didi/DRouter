package com.didi.drouter.utils;

/**
 * Created by gaowei on 2018/9/17
 */
public class TextUtil {

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static boolean equals(String s1, String s2) {
        if (s1 != null) {
            return s1.equals(s2);
        }
        return s2 == null;
    }

    public static boolean exclude(String name) {
        return  name.startsWith("android.") ||
                name.startsWith("com.google.") ||
                name.startsWith("org.apache.") ||
                name.startsWith("java.") ||
                name.startsWith("javax.");
    }

    public static boolean exclude2(String name) {
        return  name.startsWith("android/") ||
                name.startsWith("com/google/") ||
                name.startsWith("org/apache/") ||
                name.startsWith("java/") ||
                name.startsWith("javax/");
    }
}
