package com.didi.drouter.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.SystemUtil
import com.google.common.collect.ImmutableSet
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
/**
 * Created by gaowei on 2018/11/12
 */
class RouterTransform extends Transform {

    Project project
    Queue<File> compilePath
    File cacheFile
    Set<String> cachePath  //.class | .jar | .jar!/.class

    RouterTransform(Project project) {
        this.project = project
        this.cacheFile = new File(project.buildDir, "intermediates/drouter/cache")
        this.cachePath = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>())
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
        Logger.debug = project.drouter.debug
        boolean configChanged = SystemUtil.configChanged(project)
        boolean readCache = invocation.incremental && project.drouter.cache && cacheFile.exists() && !configChanged
        Logger.v("DRouterTask start"
                + " | incremental:" + invocation.incremental
                + " | readCache:" + readCache
                + " | activity:" + project.drouter.supportNoAnnotationActivity)
        if (readCache) {
            cachePath.addAll(cacheFile.readLines())
            Logger.v("read cache size: " + cachePath.size())
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
        (new RouterTask(project, compilePath, cachePath, readCache, dest, project.drouter)).run()
        FileUtils.writeLines(cacheFile, cachePath)
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
                        case Status.ADDED:
                        case Status.CHANGED:
                            if (changedSource.isFile()) {
                                FileUtils.copyFile(changedSource, changedSourceDest)
                                if (changedSource.absolutePath.endsWith(".class")) {
                                    cachePath.add(changedSource.absolutePath)
                                }
                            } else {
                                FileUtils.copyDirectory(changedSource, changedSourceDest)
                                FileUtils.listFiles(changedSource, null, true).each {
                                    if (it.absolutePath.endsWith(".class")) {
                                        cachePath.add(it.absolutePath)
                                    }
                                }
                            }
                            break
                        case Status.REMOVED:
                            if (changedSourceDest.isFile()) {
                                // for changedSource has been deleted
                                changedSourceDest.delete()
                                if (changedSource.absolutePath.endsWith(".class")) {
                                    cachePath.remove(changedSource.absolutePath)
                                }
                            } else {
                                // delete all classes under this folder
                                changedSourceDest.deleteDir()
                                Iterator<String> iterator = cachePath.iterator()
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
                        // we will remove it later when resolve jar
                        cachePath.add(changedSource.absolutePath)
                        if (jarInput.status == Status.CHANGED) {
                            // delete all classes under this jar, as we don't know which class.
                            Iterator<String> iterator = cachePath.iterator()
                            while (iterator.hasNext()) {
                                if (iterator.next().startsWith("jar:file:" + changedSource.absolutePath)) {
                                    iterator.remove()
                                }
                            }
                        }
                        break
                    case Status.REMOVED:
                        changedSourceDest.delete()
                        Iterator<String> iterator = cachePath.iterator()
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