package com.didi.drouter

import com.android.build.api.AndroidPluginVersion
import com.didi.drouter.plugin.RouterSetting
import com.didi.drouter.plugin.RouterTask
import com.didi.drouter.utils.Logger
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
    private val androidBootClasspath: ListProperty<RegularFile>,
    private val pluginVersion: AndroidPluginVersion,
    private val cacheDir: File,
    private val isWindows: Boolean,
    private val dRouterSettings: RouterSetting,
) : DefaultTask() {

    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    private val inputFiles = mutableSetOf<File>()
    private val routerDir = File(cacheDir, "router")

    @TaskAction
    fun taskAction() {
        val timeStart = System.currentTimeMillis()
        Logger.v("DRouterTask start | AndroidGradlePlugin version: ${pluginVersion.run { "$major.$minor.$micro" }}")

        // 入口
        RouterTask(
            assembleAllFile(),
            mutableSetOf(),
            false,
            routerDir,
            isWindows,
            cacheDir,
            dRouterSettings
        ).run(androidBootClasspath)
        inputFiles.add(routerDir)
        packOutputJar()

        Logger.v("Link: https://github.com/didi/DRouter")
        Logger.v("DRouterTask done, time used: " + (System.currentTimeMillis() - timeStart) / 1000f  + "s")
    }

    private fun assembleAllFile(): Queue<File> {
        val allFileQueue = ConcurrentLinkedQueue<File>()
        inputFiles.clear()
        androidBootClasspath.get().forEach {
            allFileQueue.add(it.asFile)
        }
        allJars.get().forEach {
            allFileQueue.add(it.asFile)
            inputFiles.add(it.asFile)
        }
        allDirectories.get().forEach {
            allFileQueue.add(it.asFile)
            inputFiles.add(it.asFile)
        }
        return allFileQueue
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