package com.didi.drouter.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by gaowei on 2018/11/27.
 */
public class JarUtils {

    private static Map<String, String> metaInfo = new HashMap<>();

    private static void grabApiMetaInfo(Collection<File> files) {
        for (File file : files) {
            if (!file.getName().endsWith(".jar")) {
                continue;
            }
            try {
                ZipFile zipFile = new ZipFile(file);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().equals("META-INF/drouter")) {
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            int index = line.indexOf(":");
                            if (index != -1) {
                                metaInfo.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
                            }
                        }
                        reader.close();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void grabPluginMetaInfo() {
        try {
            String classPath = JarUtils.class.getResource(JarUtils.class.getSimpleName() + ".class").toString();
            String libPath = classPath.substring(0, classPath.lastIndexOf("!"));
            String filePath = libPath + "!/META-INF/MANIFEST.MF";
            Manifest manifest = new Manifest(new URL(filePath).openStream());
            Attributes attributes = manifest.getMainAttributes();
            metaInfo.put("plugin-version", attributes.getValue("plugin-version"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void printVersion(Collection<File> files) {
        grabApiMetaInfo(files);
        grabPluginMetaInfo();
        Logger.v("current plugin-version: " + metaInfo.get("plugin-version") +
                " | api-version: " + metaInfo.get("api-version"));
    }

    public static void check(Exception e) {
        Logger.w("[Wrong message ...]");
        Logger.e(">>> " + e.getMessage() + " <<<");
        String min = metaInfo.get("plugin-min-support");
        Logger.w("Please first make sure plugin-version " + metaInfo.get("plugin-version") +
                "(current) >= " + min + "(min support)");
        Logger.w("Any question you can also put in \"qq group 1017780981\"");
    }
}
