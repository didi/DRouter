package com.didi.drouter

import com.didi.drouter.plugin.RouterSetting
import com.didi.drouter.plugin.RouterTransform
import com.didi.drouter.utils.SystemUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * Created by gaowei on 2018/9/17
 */
class RouterPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        SystemUtil.confirm(project)
        project.extensions.create('drouter', RouterSetting)
        project.android.registerTransform(new RouterTransform(project))
    }
}