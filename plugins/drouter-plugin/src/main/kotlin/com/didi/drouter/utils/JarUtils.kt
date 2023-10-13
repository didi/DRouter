package com.didi.drouter.utils

import com.android.build.gradle.AppExtension
import com.didi.drouter.utils.SystemUtil.project
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.jar.Manifest
import java.util.zip.ZipFile

/**
 * Created by gaowei on 2018/11/27.
 */
object JarUtils {

    private val metaInfo = HashMap<String, String>()

    private fun grabApiMetaInfo(files: Collection<File>) {
        for (file in files) {
            if (!file.exists() || !file.name.endsWith(".jar")) {
                continue
            }
            val zipFile = ZipFile(file)
            try {
                val entries = zipFile.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name.equals("META-INF/drouter")) {
                        val reader = BufferedReader(InputStreamReader(zipFile.getInputStream(entry)))
                        var line = reader.readLine()
                        while (line != null) {
                            val index = line.indexOf(":")
                            if (index != -1) {
                                metaInfo[line.substring(0, index).trim()] = line.substring(index + 1).trim()
                            }
                            line = reader.readLine()
                        }
                        reader.close()
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                zipFile.close()
            }
        }
    }

    private fun grabPluginMetaInfo() {
        var inputStream: InputStream? = null
        try {
            val classPath = JarUtils::class.java.getResource(JarUtils::class.java.simpleName + ".class")!!.toString()
            val libPath = classPath.substring(0, classPath.lastIndexOf("!"))
            val filePath = "$libPath!/META-INF/MANIFEST.MF"
            inputStream = URL(filePath).openStream()
            val manifest = Manifest(inputStream)
            val attributes = manifest.mainAttributes
            metaInfo["plugin-version"] = attributes.getValue("Implementation-Version") ?: "null"
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
        }
    }

    @JvmStatic
    fun printVersion(files: Collection<File>) {
        grabApiMetaInfo(files)
        grabPluginMetaInfo()
        val pluginVersion = metaInfo["plugin-version"]
        val apiVersion = metaInfo["api-version"]
        Logger.v("current plugin-version: $pluginVersion | api-version: $apiVersion")

        val appIds = HashSet<String>()
        project.extensions.findByType(AppExtension::class.java)?.applicationVariants?.all { variant ->
            appIds.add(variant.applicationId)
        }
//        Thread.start {
//            sendRequest(Arrays.toString(appIds.toArray()), pluginVersion, apiVersion)
//        }
    }

    fun check(e: Exception) {
        Logger.w("[Wrong message ...]")
        Logger.e(">>> " + e.message + " <<<")
        val min = metaInfo["plugin-min-support"]
        Logger.w(
            "Please first make sure plugin-version " + metaInfo["plugin-version"] +
                    "(current) >= " + min + "(min support)"
        )
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
