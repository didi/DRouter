package com.didi.drouter.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.SystemUtil
import com.google.common.collect.ImmutableSet
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import sun.rmi.runtime.Log

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
/**
 * Created by gaowei on 2018/11/12
 */
class RouterTransform extends Transform {

    Project project
    RouterSetting.Parse setting
    Queue<File> compilePath

    // path.class (class file)
    // jar:file:path.jar!entry.class (class in jar)
    // path.jar (jar file added in transform)
    Set<String> cachePathSet
    File tmpDir
    boolean isWindow

    RouterTransform(Project project) {
        this.project = project
        this.tmpDir = new File(project.buildDir, "intermediates/drouter")
        this.cachePathSet = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>())
        this.isWindow = System.getProperty("os.name").startsWith("Windows")
    }

    @Override
    String getName() {
        return "DRouter"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_JARS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return project.drouter.incremental
    }

    @Override
    void transform(TransformInvocation invocation) throws TransformException, InterruptedException, IOException {
        long timeStart = System.currentTimeMillis()
        this.setting = new RouterSetting.Parse(project.drouter)
        File cacheFile = new File(tmpDir, "cache")
        boolean configChanged = SystemUtil.configChanged(project, setting)
        boolean useCache = !isWindow && invocation.incremental && setting.cache && cacheFile.exists() && !configChanged
        Logger.v("DRouterTask start"
                + " | incremental:" + invocation.incremental
                + " | useCache:" + useCache)
        Logger.d(setting)
        if (useCache) {
            cachePathSet.addAll(cacheFile.readLines())
            Logger.v("read cache size: " + cachePathSet.size())
        }
        if (!invocation.incremental) {
            invocation.outputProvider.deleteAll()
        }
        compilePath = new ConcurrentLinkedQueue<>(project.android.bootClasspath)
        for (TransformInput transformInput : invocation.inputs) {
            handleDirectory(invocation, transformInput)
            handleJar(invocation, transformInput)
        }
        File dest = invocation.outputProvider.getContentLocation("DRouterTable", TransformManager.CONTENT_CLASS,
                ImmutableSet.of(QualifiedContent.Scope.PROJECT), Format.DIRECTORY)
        (new RouterTask(project, compilePath, cachePathSet, useCache, dest, tmpDir, setting, isWindow)).run()
        FileUtils.writeLines(cacheFile, cachePathSet)
        Logger.v("Link: https://github.com/didi/DRouter")
        Logger.v("DRouterTask done, time used: " + (System.currentTimeMillis() - timeStart) / 1000f  + "s")
    }

    void handleDirectory(TransformInvocation invocation, TransformInput transformInput) {
        for (DirectoryInput directoryInput : transformInput.directoryInputs) {
            compilePath.add(directoryInput.file)

            File directoryDesc = invocation.outputProvider.getContentLocation(
                    directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
            if (invocation.incremental) {
                Map<File, Status> fileStatusMap = directoryInput.changedFiles
                for (Map.Entry<File, Status> changedEntry : fileStatusMap.entrySet()) {
                    File changedSource = changedEntry.key
                    Status status = changedEntry.value
                    File changedSourceDest = new File(directoryDesc, changedSource.absolutePath.substring(
                            directoryInput.file.absolutePath.length()))
                    if (status != Status.NOTCHANGED) {
                        Logger.p("status: " + status)
                        Logger.p(" changed file: " + changedSource.absolutePath)
                        Logger.p(" transform to: " + changedSourceDest.absolutePath)
                    }
                    switch (status) {
                        case Status.ADDED:     // rename or add
                        case Status.CHANGED:   // modified
                            if (changedSource.isFile()) {
                                FileUtils.copyFile(changedSource, changedSourceDest)
                                if (changedSource.absolutePath.endsWith(".class")) {
                                    cachePathSet.add(changedSource.absolutePath)
                                }
                            } else {
                                FileUtils.copyDirectory(changedSource, changedSourceDest)
                                FileUtils.listFiles(changedSource, null, true).each {
                                    if (it.absolutePath.endsWith(".class")) {
                                        cachePathSet.add(it.absolutePath)
                                    }
                                }
                            }
                            break
                        case Status.REMOVED:   // rename or delete
                            // changed source file has been deleted
                            compilePath.remove(directoryInput.file)
                            if (changedSourceDest.isFile()) {
                                changedSourceDest.delete()
                                if (changedSource.absolutePath.endsWith(".class")) {
                                    cachePathSet.remove(changedSource.absolutePath)
                                }
                            } else {
                                // delete all classes under this folder
                                changedSourceDest.deleteDir()
                                Iterator<String> iterator = cachePathSet.iterator()
                                while (iterator.hasNext()) {
                                    if (iterator.next().startsWith(changedSource.absolutePath + "/")) {
                                        iterator.remove()
                                    }
                                }
                            }
                            break
                    }
                }
            } else {
                FileUtils.copyDirectory(directoryInput.file, directoryDesc)
            }
        }
    }

    void handleJar(TransformInvocation invocation, TransformInput transformInput) {
        for (JarInput jarInput : transformInput.jarInputs) {
            compilePath.add(jarInput.file)

            File jarDesc = invocation.outputProvider.getContentLocation(
                    jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
            if (invocation.incremental) {
                File changedSource = jarInput.file
                File changedSourceDest = jarDesc
                if (jarInput.status != Status.NOTCHANGED) {
                    Logger.p("status: " + jarInput.status)
                    Logger.p(" changed jar : " + changedSource.absolutePath)
                    Logger.p(" transform to: " + changedSourceDest.absolutePath)
                }
                switch (jarInput.status) {
                    case Status.ADDED:
                    case Status.CHANGED:
                        FileUtils.copyFile(changedSource, changedSourceDest)
                        // add again, and remove it later after resolving jar
                        cachePathSet.add(changedSource.absolutePath)
                        if (jarInput.status == Status.CHANGED) {
                            // delete all classes under this jar, as we don't know which changed.
                            Iterator<String> iterator = cachePathSet.iterator()
                            while (iterator.hasNext()) {
                                if (iterator.next().startsWith("jar:file:" + changedSource.absolutePath)) {
                                    iterator.remove()
                                }
                            }
                        }
                        break
                    case Status.REMOVED:
                        compilePath.remove(jarInput.file)
                        changedSourceDest.delete()
                        Iterator<String> iterator = cachePathSet.iterator()
                        while (iterator.hasNext()) {
                            if (iterator.next().startsWith("jar:file:" + changedSource.absolutePath)) {
                                iterator.remove()
                            }
                        }
                        break
                }
            } else {
                FileUtils.copyFile(jarInput.getFile(), jarDesc)
            }
        }
    }
}