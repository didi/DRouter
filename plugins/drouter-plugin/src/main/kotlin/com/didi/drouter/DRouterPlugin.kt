package com.didi.drouter

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.didi.drouter.plugin.RouterSetting
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.SystemUtil
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.configurationcache.extensions.capitalized

/**
 * Created by gaowei on 2023/4/17
 */
class DRouterPlugin : Plugin<Project> {

    @Suppress("PrivateApi")
    override fun apply(project: Project) {
        SystemUtil.confirm(project)
        project.extensions.create("drouter", RouterSetting::class.java)

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->

            val taskName = "${variant.name.capitalized()}DRouterTask"
            val taskContainer: TaskProvider<RouterTransform> = project.tasks.register(
                taskName,
                RouterTransform::class.java,
                androidComponents.sdkComponents.bootClasspath,
                androidComponents.pluginVersion
            )

            try {

                val scopedArtifactsOperationClass = Class.forName(
                    "com.android.build.api.artifact.impl.ScopedArtifactsImpl\$ScopedArtifactsOperationImpl"
                )
                val toTransformMethod = scopedArtifactsOperationClass.declaredMethods.find {
                    it.name.contains("toTransform") && it.parameterCount == 5
                }
                toTransformMethod?.isAccessible = true
                val use = variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskContainer)
                toTransformMethod?.invoke(
                    use,
                    ScopedArtifact.CLASSES,
                    RouterTransform::allJars,
                    RouterTransform::allDirectories,
                    RouterTransform::outputJars,
                    RouterTransform::outputClasses,
                )
            } catch (e: Exception) {
                Logger.e("DRouterPlugin reflection error, try use former jar task")
                taskContainer.get().enabled = false

                val taskContainerOld = project.tasks.register(
                    "${taskName}Jar",
                    RouterTransformJar::class.java,
                    androidComponents.sdkComponents.bootClasspath,
                    androidComponents.pluginVersion
                )
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskContainerOld).toTransform(
                        ScopedArtifact.CLASSES,
                        RouterTransformJar::allJars,
                        RouterTransformJar::allDirectories,
                        RouterTransformJar::output
                    )
            }
        }

    }
}