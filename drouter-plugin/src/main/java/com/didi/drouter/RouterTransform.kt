package com.didi.drouter

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.didi.drouter.plugin.RouterTask
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.SystemUtil
import com.didi.drouter.utils.SystemUtil.cacheDir
import com.didi.drouter.utils.SystemUtil.isWindow
import com.didi.drouter.utils.SystemUtil.setting
import org.gradle.api.Project
import org.gradle.internal.impldep.com.google.common.collect.ImmutableSet
import org.gradle.internal.impldep.org.apache.commons.io.FileUtils
import java.io.File
import java.util.Collections
import java.util.Queue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * Created by gaowei on 2018/11/12
 */
class RouterTransform(private val project: Project): Transform() {


    var compilePath: Queue<File>

    // path.class (class file)
    // jar:file:path.jar!entry.class (class in jar)
    // path.jar (jar file added in transform)
    private val cachePathSet: MutableSet<String>

    init {
        this.compilePath = ConcurrentLinkedQueue(project.extensions.findByType(AppExtension::class.java)!!.bootClasspath)
        this.cachePathSet = Collections.newSetFromMap(ConcurrentHashMap())
    }

    override fun getName(): String {
        return "DRouter"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return mutableSetOf(QualifiedContent.DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return mutableSetOf(QualifiedContent.Scope.PROJECT)
    }

    override fun isIncremental(): Boolean {
        return setting.incremental
    }

    override fun transform(invocation: TransformInvocation) {
        val timeStart = System.currentTimeMillis()

        val cacheFile = File(cacheDir, "cache")
        val configChanged = SystemUtil.configChanged()
        val useCache = !isWindow && invocation.isIncremental && setting.cache && cacheFile.exists() && !configChanged
        Logger.v("DRouterTask start"
                + " | incremental:" + invocation.isIncremental
                + " | useCache:" + useCache
                + " | gradle:" + SystemUtil.project.gradle.gradleVersion)
        Logger.d(setting)
        if (useCache) {
            cachePathSet.addAll(cacheFile.readLines())
            Logger.v("read cache size: " + cachePathSet.size)
        }
        if (!invocation.isIncremental) {
            invocation.outputProvider.deleteAll()
        }
//        compilePath = ConcurrentLinkedQueue<>(project.android.bootClasspath)
        for (transformInput in invocation.inputs) {
            handleDirectory(invocation, transformInput)
            handleJar(invocation, transformInput)
        }
        val dest = invocation.outputProvider.getContentLocation("DRouterTable", setOf(QualifiedContent.DefaultContentType.CLASSES),
            ImmutableSet.of(QualifiedContent.Scope.PROJECT), Format.DIRECTORY)
        (RouterTask(compilePath, cachePathSet, useCache, dest)).run()
        FileUtils.writeLines(cacheFile, cachePathSet)
        Logger.v("Link: https://github.com/didi/DRouter")
        Logger.v("DRouterTask done, time used: " + (System.currentTimeMillis() - timeStart) / 1000f  + "s")
    }

    private fun handleDirectory(invocation: TransformInvocation, transformInput: TransformInput) {
        for (directoryInput in transformInput.directoryInputs) {
            // directoryInput is app module root folder
            compilePath.add(directoryInput.file)
            if (!directoryInput.file.exists()) {
                Logger.w("DirectoryInput: " + directoryInput.file + "\n but it is removed, transform todo?")
            } else {
                Logger.d("DirectoryInput: " + directoryInput.file)
            }

            val directoryDesc = invocation.outputProvider.getContentLocation(
                    directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
            if (invocation.isIncremental) {
                val changedFiles = directoryInput.changedFiles
                for (changedEntry in changedFiles.entries) {
                    val changedSource = changedEntry.key
                    val status = changedEntry.value
                    // changedDest and changedSource has same suffix, such as 'class' or 'folder'
                    val changedDest = File(directoryDesc, changedSource.absolutePath.substring(directoryInput.file.absolutePath.length))
                    if (status != Status.NOTCHANGED) {
                        Logger.p("status: $status")
                        Logger.p(" changed file: " + changedSource.absolutePath)
                        Logger.p(" transform to: " + changedDest.absolutePath)
                    }
                    when (status) {
                        Status.ADDED,     // rename or add
                        Status.CHANGED-> {   // modified
                            if (changedSource.isFile) {
                                FileUtils.copyFile(changedSource, changedDest)
                                if (changedSource.absolutePath.endsWith(".class")) {
                                    cachePathSet.add(changedSource.absolutePath)
                                }
                            } else {
                                FileUtils.copyDirectory(changedSource, changedDest)
                                FileUtils.listFiles(changedSource, null, true).forEach {
                                    if (it.absolutePath.endsWith(".class")) {
                                        cachePathSet.add(it.absolutePath)
                                    }
                                }
                            }
                        }
                        Status.REMOVED-> {   // rename or delete
                            // changedSource has been deleted by system, so
                            // delete changedDest file manually
                            if (changedDest.isFile) {
                                changedDest.delete()
                                // update cache
                                if (changedSource.absolutePath.endsWith(".class")) {
                                    cachePathSet.remove(changedSource.absolutePath)
                                }
                            } else {
                                // delete changedDest folder manually
                                FileUtils.deleteDirectory(changedDest)
                                // update cache
                                val iterator = cachePathSet.iterator()
                                while (iterator.hasNext()) {
                                    if (iterator.next().startsWith(changedSource.absolutePath + "/")) {
                                        iterator.remove()
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            } else {
                FileUtils.copyDirectory(directoryInput.file, directoryDesc)
            }
        }
    }

    private fun handleJar(invocation: TransformInvocation, transformInput: TransformInput) {
        for (jarInput in transformInput.jarInputs) {
            compilePath.add(jarInput.file)

            val jarDesc = invocation.outputProvider.getContentLocation(
                    jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
            if (invocation.isIncremental) {
                val changedSource = jarInput.file
                val changedDest = jarDesc
                if (jarInput.status != Status.NOTCHANGED) {
                    Logger.p("status: " + jarInput.status)
                    Logger.p(" changed jar : " + changedSource.absolutePath)
                    Logger.p(" transform to: " + changedDest.absolutePath)
                }
                when (jarInput.status) {
                    Status.ADDED,
                    Status.CHANGED -> {
                        FileUtils.copyFile(changedSource, changedDest)
                        // add again, and remove it later after resolving jar
                        cachePathSet.add(changedSource.absolutePath)
                        if (jarInput.status == Status.CHANGED) {
                            // delete all classes under this jar, as we don't know which changed.
                            val iterator = cachePathSet.iterator()
                            while (iterator.hasNext()) {
                                if (iterator.next().startsWith("jar:file:" + changedSource.absolutePath)) {
                                    iterator.remove()
                                }
                            }
                        }
                    }
                    Status.REMOVED -> {
                        compilePath.remove(jarInput.file)
                        changedDest.delete()
                        val iterator = cachePathSet.iterator()
                        while (iterator.hasNext()) {
                            if (iterator.next().startsWith("jar:file:" + changedSource.absolutePath)) {
                                iterator.remove()
                            }
                        }
                    }
                    else -> {}
                }
            } else {
                FileUtils.copyFile(jarInput.file, jarDesc)
            }
        }
    }
}