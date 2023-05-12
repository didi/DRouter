package com.didi.drouter

import com.android.build.gradle.AppExtension
import com.didi.drouter.plugin.RouterSetting
import com.didi.drouter.utils.SystemUtil
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by gaowei on 2023/4/17
 */
class RouterPluginKt : Plugin<Project> {
    override fun apply(project: Project) {

        SystemUtil.confirm(project)
        project.extensions.create("drouter", RouterSetting::class.java)
        project.extensions.getByType(AppExtension::class.java).registerTransform(RouterTransform(project))
    }
}
