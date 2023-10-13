@file:Suppress("unused")

package com.didi.drouter

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.didi.drouter.plugin.RouterSetting
import com.didi.drouter.utils.SystemUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.register

/**
 * Created by gaowei on 2023/4/17
 */
class DRouterPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        SystemUtil.confirm(project)
        project.extensions.create("drouter", RouterSetting::class.java)

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            //先扫描所有文件，再对想要操作的字节码进行操作
            val taskContainer: TaskProvider<RouterTransform> = project.tasks.register(
                "${variant.name.capitalized()}DRouterTask",
                RouterTransform::class.java,
                androidComponents.sdkComponents.bootClasspath,
                androidComponents.pluginVersion
            )
            variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                .use(taskContainer)
                .toTransform(
                    ScopedArtifact.CLASSES,
                    RouterTransform::allJars,
                    RouterTransform::allDirectories,
                    RouterTransform::output
                )
        }
    }
}