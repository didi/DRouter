package com.didi.drouter

import com.android.build.api.AndroidPluginVersion
import com.didi.drouter.plugin.RouterTask
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.SystemUtil
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import kotlin.io.path.Path

/**
 * Created by gaowei on 2023/5/8
 */
abstract class RouterTransform @Inject constructor(
    private val androidBootClasspath: ListProperty<RegularFile>,
    private val pluginVersion: AndroidPluginVersion
) : DefaultTask() {

    @get:Classpath
    abstract val allJars: ConfigurableFileCollection

    @get:Classpath
    abstract val allDirectories: ConfigurableFileCollection

//    @get:OutputFile
//    abstract val output: RegularFileProperty

    @get:OutputDirectory
    abstract val outputJars: DirectoryProperty
    @get:OutputDirectory
    abstract val outputClasses: DirectoryProperty

    private val inputFiles = mutableSetOf<File>()
    private val routerDir = File(SystemUtil.cacheDir, "router")

    @TaskAction
    fun taskAction() {
        val timeStart = System.currentTimeMillis()
        Logger.v("DRouterTask start | AndroidGradlePlugin version: ${pluginVersion.run { "$major.$minor.$micro" }}")
        // 入口
        RouterTask(assembleAllFile(), mutableSetOf(), false, routerDir).run(androidBootClasspath)
//        val time = System.currentTimeMillis()
        // 测试耗时 31 ms
        copyToOutput()
//        Logger.v("copyToOutput time: ${System.currentTimeMillis() - time}ms")
        // 测试耗时 3 ms 但编译报错
//        linkToOutput()
//        Logger.v("linkToOutput time: ${System.currentTimeMillis() - time}ms")

        Logger.v("Link: https://github.com/didi/DRouter")
        Logger.v("DRouterTask done, time used: " + (System.currentTimeMillis() - timeStart) / 1000f  + "s")
    }

    private fun assembleAllFile(): Queue<File> {
        val allFileQueue = ConcurrentLinkedQueue<File>()
        inputFiles.clear()
        androidBootClasspath.get().forEach {
            allFileQueue.add(it.asFile)
        }
        allJars.files.forEach {
            allFileQueue.add(it)
            inputFiles.add(it)
        }
        allDirectories.files.forEach {
            allFileQueue.add(it)
            inputFiles.add(it)
        }
        return allFileQueue
    }

    private fun copyToOutput() {
        val jarDirFile = outputJars.get().asFile
        if(jarDirFile.exists()) {
            jarDirFile.deleteRecursively()
        }
        jarDirFile.mkdirs()
        allJars.files.forEachIndexed {index, it->
            try {
                it.copyTo(File(jarDirFile, it.name), false)
            } catch (e: FileAlreadyExistsException) {
                it.copyTo(File(jarDirFile, "${it.nameWithoutExtension}$index.${it.extension}"), false)
            }
        }

        val classDirFile = outputClasses.get().asFile
        if(classDirFile.exists()) {
            classDirFile.deleteRecursively()
        }
        classDirFile.mkdirs()

        allDirectories.forEach { dir->
            dir.copyRecursively(classDirFile, false)
        }

        routerDir.copyRecursively(classDirFile, false)
    }

    private fun linkToOutput() {
        val jarDirFile = outputJars.get().asFile
        if(jarDirFile.exists()) {
            jarDirFile.deleteRecursively()
        }
        jarDirFile.mkdirs()
        allJars.files.forEach {
            Files.createSymbolicLink(
                File(jarDirFile, it.name).toPath(),
                it.toPath(),
            )
        }

        val classDirFile = outputClasses.get().asFile
        if(classDirFile.exists()) {
            classDirFile.deleteRecursively()
        }
        classDirFile.mkdirs()

        allDirectories.forEach { dir->
            Files.createSymbolicLink(
                File(classDirFile, dir.name).toPath(),
                dir.toPath(),
            )
        }

        Files.createSymbolicLink(
            File(classDirFile, routerDir.name).toPath(),
            routerDir.toPath(),
        )
    }

}