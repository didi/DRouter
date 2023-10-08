package com.didi.drouter

import com.didi.drouter.plugin.RouterSetting
import com.didi.drouter.utils.SystemUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized

/**
 * Created by gaowei on 2023/4/17
 */
class RouterPluginKt : Plugin<Project> {
    override fun apply(project: Project) {

        SystemUtil.confirm(project)
        project.extensions.create("drouter", RouterSetting::class.java)

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            val taskProvider = project.tasks.register(
                "${variant.name.capitalized()}DRouterTask", RouterTransform::class.java, androidComponents)

            variant.artifacts.forScope(ScopedArtifacts.Scope.ALL).use(taskProvider)
                .toTransform(ScopedArtifact.CLASSES,
                    RouterTransform::allJars,
                    RouterTransform::allDirectories,
                    RouterTransform::output)
        }
    }
}
