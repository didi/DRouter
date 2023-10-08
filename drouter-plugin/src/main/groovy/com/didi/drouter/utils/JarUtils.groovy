package com.didi.drouter.utils

import com.android.build.gradle.api.ApplicationVariant
import org.gradle.api.Project

import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
/**
 * Created by gaowei on 2018/11/27.
 */
class JarUtils {

    private static Map<String, String> metaInfo = new HashMap<>()

    private static void grabApiMetaInfo(Collection<File> files) {
        for (File file : files) {
            if (!file.getName().endsWith(".jar")) {
                continue
            }
            ZipFile zipFile = new ZipFile(file)
            try {
                Enumeration<? extends ZipEntry> entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement()
                    if (!entry.isDirectory() && entry.getName().equals("META-INF/drouter")) {
                        BufferedReader reader =
                                new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))
                        String line
                        while ((line = reader.readLine()) != null) {
                            int index = line.indexOf(":")
                            if (index != -1) {
                                metaInfo.put(line.substring(0, index).trim(), line.substring(index + 1).trim())
                            }
                        }
                        reader.close()
                        break
                    }
                }
            } catch (Exception e) {
                e.printStackTrace()
            } finally {
                zipFile.close()
            }
        }
    }

    private static void grabPluginMetaInfo() {
        InputStream inputStream
        try {
            String classPath = JarUtils.class.getResource(JarUtils.class.getSimpleName() + ".class").toString()
            String libPath = classPath.substring(0, classPath.lastIndexOf("!"))
            String filePath = libPath + "!/META-INF/MANIFEST.MF"
            inputStream = new URL(filePath).openStream()
            Manifest manifest = new Manifest(inputStream)
            Attributes attributes = manifest.getMainAttributes()
            metaInfo.put("plugin-version", attributes.getValue("plugin-version"))
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            if (inputStream != null) {
                inputStream.close()
            }
        }
    }

    static void printVersion(Project project, Collection<File> files) {
        grabApiMetaInfo(files)
        grabPluginMetaInfo()
        def pluginVersion = metaInfo.get("plugin-version")
        def apiVersion = metaInfo.get("api-version")
        Logger.v("current plugin-version: " + pluginVersion + " | api-version: " + apiVersion)

        Set<String> appIds = new HashSet<>()
        project.android.applicationVariants.all { ApplicationVariant variant ->
            appIds.add(variant.applicationId)
        }
//        Thread.start {
//            sendRequest(Arrays.toString(appIds.toArray()), pluginVersion, apiVersion)
//        }
    }

    static void check(Exception e) {
        Logger.e("[Wrong message ...]")
        e.printStackTrace()
        String min = metaInfo.get("plugin-min-support")
        Logger.w("Please first make sure plugin-version " + metaInfo.get("plugin-version") +
                "(current) >= " + min + "(min support)")
        Logger.w("Any question you can call \"GaoWei\" from weixin \"gwball\"")
    }

//    private static void sendRequest(String appId, String pluginVersion, String apiVersion) {
//        try {
//            def link = "https://czp.xiaojukeji.com/api/content/v1/compile?"
//            def args = "appId=" + URLEncoder.encode(appId, "UTF-8") +
//                    "&pluginVersion=" + URLEncoder.encode(pluginVersion == null ? "" : pluginVersion, "UTF-8") +
//                    "&apiVersion=" + URLEncoder.encode(apiVersion, "UTF-8")
//            def url = new URL(link + args)
//            def connection = (HttpURLConnection) url.openConnection()
//            connection.setRequestMethod("GET")
//            connection.setConnectTimeout(15000)
//            connection.setReadTimeout(60000)
//            def response = connection.inputStream.text
//        } catch(Exception ignore) {
//        }
//        //Logger.d("compile statistic response = " + response)
//    }
}
