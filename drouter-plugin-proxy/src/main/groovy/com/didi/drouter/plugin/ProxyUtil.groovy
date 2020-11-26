package com.didi.drouter.plugin

import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppPlugin
import org.gradle.api.Project

import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
/**
 * Created by gaowei on 2018/11/14
 */
class ProxyUtil {

    static void confirm(Project project) {
        if (!project.plugins.hasPlugin(AppPlugin)) {
            throw new RuntimeException("DRouterPlugin: please apply \'com.android.application\' first")
        }
    }

    static String getPluginVersion(TransformInvocation invocation) {
        for (TransformInput transformInput : invocation.inputs) {
            for (JarInput jarInput : transformInput.jarInputs) {
                File file = jarInput.file
                if (!file.getName().endsWith(".jar")) {
                    continue
                }
                try {
                    ZipFile zipFile = new ZipFile(file)
                    Enumeration<? extends ZipEntry> entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement()
                        if (!entry.isDirectory() && entry.getName() == "META-INF/drouter") {
                            Map<String, String> metaInfo = new HashMap<>()
                            BufferedReader reader = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))
                            String line
                            while ((line = reader.readLine()) != null) {
                                int index = line.indexOf(":")
                                if (index != -1) {
                                    metaInfo.put(line.substring(0, index).trim(), line.substring(index + 1).trim())
                                }
                            }
                            reader.close()
                            return metaInfo.get("plugin-min-support")
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    static Class<?> loadClass(String name, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(name)
        } catch (ClassNotFoundException ignored) {
            return null
        }
    }

    static String getProxyVersion() {
        try {
            String classPath = ProxyUtil.class.getResource(ProxyUtil.class.getSimpleName() + ".class").toString()
            String libPath = classPath.substring(0, classPath.lastIndexOf("!"))
            String filePath = libPath + "!/META-INF/MANIFEST.MF"
            Manifest manifest = new Manifest(new URL(filePath).openStream())
            Attributes attributes = manifest.getMainAttributes()
            return attributes.getValue("proxy-version")
        } catch (Exception e) {
            e.printStackTrace()
        }
        return null
    }

    static class Logger {

        static void v(Object msg) {
            System.out.println("\033[36m" + msg + "\033[0m")
        }

        static void e(Object msg) {
            System.out.println("\033[31;1m" + msg + "\033[0m")
        }
    }
}