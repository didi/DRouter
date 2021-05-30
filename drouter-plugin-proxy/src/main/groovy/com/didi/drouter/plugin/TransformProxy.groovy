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
//    static String HOST = "https://jcenter.bintray.com/" +
//            "com/didi/drouter/drouter-plugin/%s/drouter-plugin-%s.jar"
    static String HOST = "https://repo1.maven.org/maven2/" +
            "io/github/didi/drouter-plugin/%s/drouter-plugin-%s.jar"

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
                URL pluginUrl = new URL(String.format(HOST, pluginVersion, pluginVersion))
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
        if (!invocation.incremental) {
            invocation.outputProvider.deleteAll()
        }
        Queue<File> compilePath = new ConcurrentLinkedQueue<>()
        for (TransformInput transformInput : invocation.inputs) {
            for (JarInput jarInput : transformInput.jarInputs) {
                compilePath.add(jarInput.file)
                File dest = invocation.outputProvider.getContentLocation(
                        jarInput.name, jarInput.contentTypes,
                        jarInput.scopes, Format.JAR)
                try {
                    FileUtils.copyFile(jarInput.file, dest)
                } catch (IOException e) {
                    throw new RuntimeException(e)
                }
            }
            for (DirectoryInput directoryInput : transformInput.directoryInputs) {
                compilePath.add(directoryInput.file)
                File dest = invocation.outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes,
                        directoryInput.scopes, Format.DIRECTORY)
                try {
                    FileUtils.copyDirectory(directoryInput.file, dest)
                } catch (IOException e) {
                    throw new RuntimeException(e)
                }
            }
        }
    }
}