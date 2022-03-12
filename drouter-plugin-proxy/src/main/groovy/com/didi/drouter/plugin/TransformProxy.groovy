package com.didi.drouter.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentLinkedQueue
/**
 * Created by gaowei on 2018/11/12
 */
class TransformProxy extends Transform {

    Project project
    static String HOST = "%s/io/github/didi/drouter-plugin/%s/drouter-plugin-%s.jar"

    TransformProxy(Project project) {
        this.project = project
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
        ProxyUtil.Logger.v("plugin proxy version: " + ProxyUtil.getProxyVersion())
        String pluginVersion = ProxyUtil.getPluginVersion(invocation)
        if (pluginVersion != null) {
            File pluginJar = new File(project.rootDir, ".gradle/drouter/drouter-plugin-${pluginVersion}.jar")
            if (!pluginJar.exists()) {
                URL pluginUrl = new URL(String.format(HOST,
                        !ProxyUtil.isEmpty(project.drouter.repo) ?
                                project.drouter.repo : "https://repo1.maven.org/maven2",
                        pluginVersion, pluginVersion))
                try {
                    FileUtils.copyURLToFile(pluginUrl, pluginJar)
                    ProxyUtil.Logger.v("plugin url: " + pluginUrl.toString())
                    ProxyUtil.Logger.v("plugin download success: " + pluginJar.path)
                } catch (Exception e) {
                    ProxyUtil.Logger.e("Error: drouter-plugin download fail, " + pluginUrl.toString())
                    throw e
                }
            }
            if (pluginJar.exists()) {
                URLClassLoader newLoader = new URLClassLoader([pluginJar.toURI().toURL()] as URL[], getClass().classLoader)
                Class<?> transformClass = newLoader.loadClass("com.didi.drouter.plugin.RouterTransform")
                ClassLoader threadLoader = Thread.currentThread().getContextClassLoader()
                Thread.currentThread().setContextClassLoader(newLoader)
                Constructor constructor = transformClass.getConstructor(Project.class)
                Transform transform = (Transform) constructor.newInstance(project)
                transform.transform(invocation)
                Thread.currentThread().setContextClassLoader(threadLoader)
                return
            } else {
                ProxyUtil.Logger.e("Error: there is no drouter-plugin jar")
            }
        } else {
            ProxyUtil.Logger.e("Error: there is no drouter-plugin version")
        }
        copyFile(invocation)
    }

    static void copyFile(TransformInvocation invocation) {
        invocation.outputProvider.deleteAll()
        for (TransformInput transformInput : invocation.inputs) {
            for (JarInput jarInput : transformInput.jarInputs) {
                if (jarInput.file.exists()) {
                    File dest = invocation.outputProvider.getContentLocation(
                            jarInput.name, jarInput.contentTypes,
                            jarInput.scopes, Format.JAR)
                    FileUtils.copyFile(jarInput.file, dest)
                }
            }
            for (DirectoryInput directoryInput : transformInput.directoryInputs) {
                if (directoryInput.file.exists()) {
                    File dest = invocation.outputProvider.getContentLocation(
                            directoryInput.name, directoryInput.contentTypes,
                            directoryInput.scopes, Format.DIRECTORY)
                    FileUtils.copyDirectory(directoryInput.file, dest)
                }
            }
        }
    }
}