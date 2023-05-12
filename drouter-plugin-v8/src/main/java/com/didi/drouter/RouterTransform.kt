package com.didi.drouter


import com.android.build.api.variant.AndroidComponentsExtension
import com.didi.drouter.plugin.RouterTask
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.SystemUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import javax.inject.Inject

/**
 * Created by gaowei on 2023/5/8
 */
abstract class RouterTransform @Inject constructor(
    private val androidComponents: AndroidComponentsExtension<*, *, *>) : DefaultTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val inputFiles = mutableSetOf<File>()
    private val routerDir = File(SystemUtil.cacheDir, "router")

    @TaskAction
    fun taskAction() {
        val timeStart = System.currentTimeMillis()
        Logger.v("DRouterTask start"
                + " | gradle:" + androidComponents.pluginVersion.run { "$major.$minor.$micro" })
        RouterTask(assembleClassPath(), mutableSetOf(), false, routerDir).run()
        inputFiles.add(routerDir)
        packOutputJar()
        Logger.v("Link: https://github.com/didi/DRouter")
        Logger.v("DRouterTask done, time used: " + (System.currentTimeMillis() - timeStart) / 1000f  + "s")
    }

    @Suppress("UnstableApiUsage")
    private fun assembleClassPath(): Queue<File> {
        val classpath = ConcurrentLinkedQueue<File>()
        inputFiles.clear()
        androidComponents.sdkComponents.bootClasspath.get().forEach {
            classpath.add(it.asFile)
        }
        allJars.get().forEach {
            classpath.add(it.asFile)
            inputFiles.add(it.asFile)
        }
        allDirectories.get().forEach {
            classpath.add(it.asFile)
            inputFiles.add(it.asFile)
        }
        return classpath
    }

    private fun packOutputJar() {
        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))
        val insertTag = mutableSetOf<String>()
        inputFiles.forEach { input ->
            if (input.isFile && input.name.lowercase().endsWith(".jar")) {
                val jarFile = JarFile(input)
                for (jarEntry in jarFile.entries()) {
                    if (!insertTag.contains(jarEntry.name)) {
                        insertTag.add(jarEntry.name)
                        jarOutput.putNextEntry(JarEntry(jarEntry.name))
                        jarFile.getInputStream(jarEntry).use {
                            it.copyTo(jarOutput)
                        }
                        jarOutput.closeEntry()
                    }
                }
                jarFile.close()
            } else {
                // input is dir
                input.walk().filter { it.isFile }.forEach { file ->
                    val relativePath = input.toURI().relativize(file.toURI()).path
                    jarOutput.putNextEntry(JarEntry(relativePath.replace(File.separatorChar, '/')))
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(jarOutput)
                    }
                    jarOutput.closeEntry()
                }
            }
        }
        jarOutput.close()
    }
}