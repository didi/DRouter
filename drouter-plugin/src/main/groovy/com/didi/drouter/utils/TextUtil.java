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

    public static boolean excludePackageClass(String name) {
        return  name.startsWith("android.") ||
                name.startsWith("androidx.") ||
                name.startsWith("com.google.") ||
                name.startsWith("org.apache.") ||
                name.startsWith("org.intellij.") ||
                name.startsWith("java.") ||
                name.startsWith("javax.") ||
                name.startsWith("kotlin.");
    }

    public static boolean excludeJarNameFile(String name) {
        return  name.equals("android.jar") ||
                name.contains("kotlin-") ||
                name.contains("jetified-") ||
                name.contains("appcompat-") ||
                name.contains("multidex-") ||
                name.contains("animated-vector-") ||
                name.contains("gson-") ||
                name.contains("support-");
    }

    public static boolean excludePackageClassInJar(String name) {
        return  name.startsWith("android/") ||
                name.startsWith("androidx/") ||
                name.startsWith("com/google/") ||
                name.startsWith("org/apache/") ||
                name.startsWith("org/intellij/") ||
                name.startsWith("java/") ||
                name.startsWith("javax/") ||
                name.startsWith("kotlin/") ||
                name.startsWith("META-INF/");
    }
}
