package com.didi.drouter.plugin

import com.didi.drouter.generator.ClassClassify
import com.didi.drouter.utils.JarUtils.check
import com.didi.drouter.utils.JarUtils.printVersion
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.StoreUtil
import com.didi.drouter.utils.TextUtil
import javassist.ClassPool
import javassist.CtClass
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URL
import java.util.Queue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.jar.JarFile

/**
 * Created by gaowei on 2018/9/17
 */
class RouterTask(
    private val compileClassPath: Queue<File>, //.class | .jar | .jar!/class
    private val cachePathSet: MutableSet<String>,
    private val useCache: Boolean,
    private val routerDir: File,
    isWindows: Boolean,
    cacheDir: File,
    private val dRouterSettings: RouterSetting,
) {
    private val wTmpDir: File? =
        if (isWindows) File(cacheDir, System.currentTimeMillis().toString()) else null
    private var pool: ClassPool? = null
    private var classClassify: ClassClassify? = null
    private val executor = Executors.newCachedThreadPool()
    private val cpuCount = Runtime.getRuntime().availableProcessors()
    private val count = AtomicInteger()

    fun run(bootClasspath: ListProperty<RegularFile>) {
        StoreUtil.clear()
        printVersion(compileClassPath)
        // https://github.com/didi/DRouter/issues/56
        if (wTmpDir != null) {
            ClassPool.cacheOpenedJarFile = false
        }
        pool = ClassPool().also {
            // 添加 android.jar
            it.appendClassPath(bootClasspath.get()[0].toString())
            classClassify = ClassClassify(it, RouterSetting.Parse(dRouterSettings))
        }
        startExecute()
    }

    private fun startExecute() {
        val tempPool = pool ?: return
        val tempClassClassify = classClassify ?: return
        try {
            var timeStart = System.currentTimeMillis()
            for (file: File in compileClassPath) {
                tempPool.appendClassPath(file.absolutePath)
            }
            if (useCache) {
                loadCachePaths(cachePathSet)
            } else {
                loadFullPaths(compileClassPath)
            }
            Logger.d("load class used: " + (System.currentTimeMillis() - timeStart) + "ms")
            timeStart = System.currentTimeMillis()
            tempClassClassify.generatorRouter(routerDir)
            Logger.d("generator router table used: " + (System.currentTimeMillis() - timeStart) + "ms")
            Logger.v("scan class size: " + count.get() + " | router class size: " + cachePathSet.size)
        } catch (e: Exception) {
            check(e)
            throw GradleException("Could not generate d_router table\n" + e.message, e)
        } finally {
            executor.shutdown()
            FileUtils.deleteQuietly(wTmpDir)
        }
    }

    @Throws(IOException::class)
    private fun loadCachePaths(cachePath: Set<String>) {
        // multi-thread for entry class of same jar may conflict
        Logger.d("start load cache with incremental:")
        for (path: String in cachePath) {
            Logger.d("  path: $path")
            loadCachePath(path)
        }
    }

    @Throws(ExecutionException::class, InterruptedException::class, IOException::class)
    private fun loadFullPaths(files: Queue<File>) {
        if (!RouterSetting.Parse.debug) {
            val taskList = ArrayList<Future<Int>>()
            for (i in 0 until cpuCount) {
                taskList.add(executor.submit(Callable {
                    var file: File? = null
                    var loadCount = 0
                    while ((files.poll()?.also { file = it }) != null) {
                        file?.also{
                            loadFullPath(it)
                            loadCount++
                        }
                    }
                    loadCount
                }))
            }
            for (task: Future<Int> in taskList) {
                task.get()
//                Logger.v("load full count: ${task.get()")
            }
        } else {
            Logger.d("start load full:")
            for (file: File in files) {
                Logger.d("  path: " + file.absolutePath)
                loadFullPath(file)
            }
        }
    }

    @Throws(IOException::class)
    private fun loadCachePath(path: String) {
        if (path.startsWith("jar:file:")) {
            // class in jar file
            resolveCachedClassInJar(path)
        } else if (path.endsWith(".class")) {
            // class file
            resolveClassFile(File(path))
        } else if (path.endsWith(".jar")) {
            // jar file added in transform
            resolveJarFile(File(path))
            cachePathSet.remove(path)
        } else {
            throw RuntimeException("is cached dir ?")
        }
    }

    @Throws(IOException::class)
    private fun loadFullPath(file: File) {
        if (file.isDirectory) {
            val files = file.listFiles()
            if (files != null) {
                for (childFile: File in files) {
                    loadFullPath(childFile)
                }
            }
        } else if (file.name.endsWith(".class")) {
            resolveClassFile(file)
        } else if (file.name.endsWith(".jar")) {
            resolveJarFile(file)
        }
    }

    @Throws(IOException::class)
    private fun resolveClassFile(file: File) {
        count.incrementAndGet()
        val stream = FileInputStream(createFile(file, ".class"))
        val ctClass: CtClass?
        try {
            ctClass = pool?.makeClass(stream)
        } catch (e: Exception) {
            Logger.w(
                ("drouter resolve class error," +
                        " file=" + file.absolutePath +
                        " exception=" + e.message)
            )
            return
        } finally {
            stream.close()
        }

        if (ctClass == null) return

        if (!TextUtil.excludePackageClass(ctClass.name)) {
            if (classClassify?.doClassify(ctClass) == true) {
                cachePathSet.add(file.absolutePath)
            } else if (useCache) {
                cachePathSet.remove(file.absolutePath)
            }
        }
    }

    @Throws(IOException::class)
    private fun resolveJarFile(file: File) {
        val tempPool = pool ?: return
        val tempClassClassify = classClassify ?: return
        if (!TextUtil.excludeJarNameFile(file.name)) {
            val jar = JarFile(createFile(file, ".jar"))
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val entryName = entry.name
                if (entryName.endsWith(".class")) {
                    count.incrementAndGet()
                    if (!TextUtil.excludePackageClassInJar(entryName)) {
                        try {
                            jar.getInputStream(entry).use { stream ->
                                val path: String =
                                    "jar:file:" + file.absolutePath + "!/" + entryName
                                val clz: CtClass = tempPool.makeClass(stream)
                                if (tempClassClassify.doClassify(clz)) {
                                    cachePathSet.add(path)
                                } else if (useCache) {
                                    cachePathSet.remove(path)
                                } else {

                                }
                            }
                        } catch (e: Exception) {
                            Logger.w(
                                ("drouter resolve jar class error," +
                                        " jar=" + file.absolutePath +
                                        " entry=" + entry.name +
                                        " exception=" + e.message)
                            )
                            throw e
                        }
                    }
                }
            }
            jar.close()
        }
    }

    @Throws(IOException::class)
    private fun resolveCachedClassInJar(path: String) {
        val tempPool = pool ?: return
        val tempClassClassify = classClassify ?: return
        count.incrementAndGet()
        try {
            URL(path).openStream().use { stream ->
                val ctClass: CtClass = tempPool.makeClass(stream)
                if (!tempClassClassify.doClassify(ctClass)) {
                    cachePathSet.remove(path)
                }
            }
        } catch (e: Exception) {
            Logger.e(
                ("drouter resolve jar class error," +
                        " entry=" + path +
                        " exception=" + e.message)
            )
            throw e
        }
    }

    @Throws(IOException::class)
    private fun createFile(file: File, suffix: String): File {
        if (wTmpDir != null) {
            val wFile = File(
                wTmpDir, String.format(
                    "%s-%s%s",
                    Thread.currentThread().id,
                    System.currentTimeMillis(),
                    suffix
                )
            )
            FileUtils.copyFile(file, wFile)
            return wFile
        }
        return file
    }
}
