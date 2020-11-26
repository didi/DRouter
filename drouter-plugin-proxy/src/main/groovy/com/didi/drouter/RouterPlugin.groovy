package com.didi.drouter

import com.didi.drouter.plugin.RouterSetting
import com.didi.drouter.plugin.TransformProxy
import com.didi.drouter.plugin.ProxyUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * Created by gaowei on 2018/9/17
 */
class RouterPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        ProxyUtil.confirm(project)
        project.extensions.create('drouter', RouterSetting)
        project.android.registerTransform(new TransformProxy(project))
    }
}