package com.didi.drouter.utils

import com.android.build.gradle.AppPlugin
import com.didi.drouter.plugin.RouterSetting
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.Charset

/**
 * Created by gaowei on 2018/11/14
 */
object SystemUtil {

    lateinit var setting: RouterSetting.Parse
    lateinit var project: Project
    lateinit var cacheDir: File
    var isWindow = false

    fun confirm(project: Project) {
        if (!project.plugins.hasPlugin(AppPlugin::class.java)) {
            throw RuntimeException("DRouterPlugin: please apply \'com.android.application\' first")
        }

        SystemUtil.project = project
        cacheDir = File(project.layout.buildDirectory.get().asFile, "intermediates/drouter")
        isWindow = System.getProperty("os.name").startsWith("Windows")

        project.gradle.projectsEvaluated {
            setting = RouterSetting.Parse(project.extensions.getByType(RouterSetting::class.java))
        }
    }

    fun configChanged(): Boolean {
        val configFile = File(project.layout.buildDirectory.get().asFile, "intermediates/drouter/config")
        val changed =
            if (!configFile.exists()) {
                true
            } else {
                configFile.readText().toBoolean() != setting.supportNoAnnotationActivity
            }
        FileUtils.write(configFile, setting.supportNoAnnotationActivity.toString(), Charset.defaultCharset())
        return changed
    }
}