package com.didi.drouter

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
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

    @Suppress("PrivateApi")
    override fun apply(project: Project) {
        SystemUtil.confirm(project)
        project.extensions.create("drouter", RouterSetting::class.java)

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            val taskContainer: TaskProvider<RouterTransform> = project.tasks.register(
                "${variant.name.capitalized()}DRouterTask",
                RouterTransform::class.java,
                androidComponents.sdkComponents.bootClasspath,
                androidComponents.pluginVersion
            )

            val scopedArtifactsOperationClass = Class.forName(
                "com.android.build.api.artifact.impl.ScopedArtifactsImpl\$ScopedArtifactsOperationImpl"
            )
            val toTransformMethod = scopedArtifactsOperationClass.declaredMethods.find {
                it.name.contains("toTransform") && it.parameterCount == 5
            }
            toTransformMethod?.isAccessible = true
            toTransformMethod?.invoke(
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskContainer),
                ScopedArtifact.CLASSES,
                RouterTransform::allJars,
                RouterTransform::allDirectories,
                RouterTransform::outputJars,
                RouterTransform::outputClasses,
            )
        }

    }
}