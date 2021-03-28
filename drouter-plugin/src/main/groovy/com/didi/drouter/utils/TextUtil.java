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

    public static boolean excludeClass(String name) {
        return  name.startsWith("android.") ||
                name.startsWith("com.google.") ||
                name.startsWith("org.apache.") ||
                name.startsWith("java.") ||
                name.startsWith("javax.");
    }

    public static boolean excludeJarFile(String name) {
        return  name.equals("android.jar") ||
                name.startsWith("kotlin-stdlib-") ||
                name.startsWith("appcompat-") ||
                name.startsWith("multidex-") ||
                name.startsWith("animated-vector-") ||
                name.startsWith("gson-") ||
                name.startsWith("support-");
    }

    public static boolean excludeJarEntry(String name) {
        return  name.startsWith("android/") ||
                name.startsWith("androidx/") ||
                name.startsWith("com/google/") ||
                name.startsWith("org/apache/") ||
                name.startsWith("org/intellij/") ||
                name.startsWith("java/") ||
                name.startsWith("javax/") ||
                name.startsWith("kotlin/");
    }
}
