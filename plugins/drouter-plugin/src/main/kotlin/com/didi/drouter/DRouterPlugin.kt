package com.didi.drouter

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.didi.drouter.plugin.RouterSetting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import java.io.File

/**
 * Created by gaowei on 2023/4/17
 */
class DRouterPlugin : Plugin<Project> {

    @Suppress("PrivateApi")
    override fun apply(project: Project) {

        val cacheDir = File(project.layout.buildDirectory.get().asFile, "intermediates/drouter")
        val isWindow = System.getProperty("os.name").startsWith("Windows")
        val dRouterSettings: RouterSetting =
            project.extensions.create("drouter", RouterSetting::class.java)

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->

            val taskName = "${variant.name.capitalized()}DRouterTask"
            val taskContainer = project.tasks.register(
                taskName,
                RouterTransform::class.java,
                androidComponents.sdkComponents.bootClasspath,
                androidComponents.pluginVersion,
                cacheDir,
                isWindow,
                dRouterSettings,
            )
            variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                .use(taskContainer).toTransform(
                    ScopedArtifact.CLASSES,
                    RouterTransform::allJars,
                    RouterTransform::allDirectories,
                    RouterTransform::output
                )
        }
    }
}