package com.didi.drouter.utils

import com.android.build.gradle.AppPlugin
import com.didi.drouter.plugin.RouterSetting
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
/**
 * Created by gaowei on 2018/11/14
 */
class SystemUtil {

    static void confirm(Project project) {
        if (!project.plugins.hasPlugin(AppPlugin)) {
            throw new RuntimeException("DRouterPlugin: please apply \'com.android.application\' first")
        }
    }

    static boolean configChanged(Project project, RouterSetting.Parse setting) {
        boolean changed
        File configFile = new File(project.buildDir, "intermediates/drouter/config")
        if (!configFile.exists()) {
            changed = true
        } else {
            changed = Boolean.valueOf(configFile.text) != setting.isSupportNoAnnotationActivity()
        }
        FileUtils.write(configFile, String.valueOf(setting.isSupportNoAnnotationActivity()))
        return changed
    }



}