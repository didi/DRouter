package com.didi.drouter.utils

/**
 * Created by gaowei on 2018/9/17
 */
object TextUtil {
    fun isEmpty(str: CharSequence?): Boolean {
        return str == null || str.length == 0
    }

    fun equals(s1: String?, s2: String?): Boolean {
        return if (s1 != null) {
            s1 == s2
        } else s2 == null
    }

    fun excludePackageClass(name: String): Boolean {
        return name.startsWith("android.") ||
                name.startsWith("androidx.") ||
                name.startsWith("com.google.") ||
                name.startsWith("org.apache.") ||
                name.startsWith("org.intellij.") ||
                name.startsWith("java.") ||
                name.startsWith("javax.") ||
                name.startsWith("kotlin.")
    }

    fun excludeJarNameFile(name: String): Boolean {
        return name == "android.jar"
    }

    fun excludePackageClassInJar(name: String): Boolean {
        return name.startsWith("android/") ||
                name.startsWith("androidx/") ||
                name.startsWith("com/google/") ||
                name.startsWith("org/apache/") ||
                name.startsWith("org/intellij/") ||
                name.startsWith("java/") ||
                name.startsWith("javax/") ||
                name.startsWith("kotlin/") ||
                name.startsWith("META-INF/")
    }
}
