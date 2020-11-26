package com.didi.drouter.plugin;

import com.didi.drouter.generator.ClassClassify;
import com.didi.drouter.utils.JarUtils;
import com.didi.drouter.utils.Logger;
import com.didi.drouter.utils.StoreUtil;
import com.didi.drouter.utils.TextUtil;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * Created by gaowei on 2018/9/17
 */
public class RouterTask {

    private Queue<File> compileClassPath;
    private Set<String> cachePath; //.class | .jar | .jar!/class
    private boolean useCache;
    private File routerDir;
    private RouterSetting setting;

    private ClassPool pool;
    private ClassClassify classClassify;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private AtomicInteger count = new AtomicInteger();

    RouterTask(Queue<File> compileClassPath, Set<String> cachePath, boolean useCache,
               File routerDir, RouterSetting setting) {
        this.compileClassPath = compileClassPath;
        this.cachePath = cachePath;
        this.useCache = useCache;
        this.routerDir = routerDir;
        this.setting = setting;
    }

    void run() {
        StoreUtil.clear();
        JarUtils.printVersion(compileClassPath);
        pool = new ClassPool();
        classClassify = new ClassClassify(pool, setting);
        startExecute();
    }

    private void startExecute() {
        try {
            long timeStart = System.currentTimeMillis();
            for (File file : compileClassPath) {
                pool.appendClassPath(file.getAbsolutePath());
            }
            if (useCache) {
                loadCachePaths(cachePath);
            } else {
                loadFullPaths(compileClassPath);
            }
            Logger.d("load class used: " + (System.currentTimeMillis() - timeStart)  + "ms");
            timeStart = System.currentTimeMillis();
            classClassify.generatorRouter(routerDir);
            Logger.d("generator router table used: " + (System.currentTimeMillis() - timeStart) + "ms");
            Logger.v("scan class size: " + count.get() + " | router class size: " + cachePath.size());
        } catch (Exception e) {
            JarUtils.check(e);
            String message = e.getMessage();
            if (message == null || !message.startsWith("Class:")) {
                e.printStackTrace();
            }
            throw new GradleException("DRouter: Could not generate router table\n" + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private void loadCachePaths(final Set<String> cachePath) throws ExecutionException, InterruptedException {
        final Queue<String> pathQueue = new ConcurrentLinkedQueue<>(cachePath);
        final List<Future<Void>> taskList = new ArrayList<>();
        for (int i = 0; i < CPU_COUNT; i++) {
            taskList.add(executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    String path;
                    while ((path = pathQueue.poll()) != null) {
                        loadCacheClass(path);
                    }
                    return null;
                }
            }));
        }
        for (Future<Void> task : taskList) {
            task.get();
        }
    }

    private void loadFullPaths(final Queue<File> files) throws ExecutionException, InterruptedException {
        final List<Future<Void>> taskList = new ArrayList<>();
        for (int i = 0; i < CPU_COUNT; i++) {
            taskList.add(executor.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    File file;
                    while ((file = files.poll()) != null) {
                        loadFullClass(file);
                    }
                    return null;
                }
            }));
        }
        for (Future<Void> task : taskList) {
            task.get();
        }
    }

    private void loadCacheClass(String path) throws IOException {
        if (path.startsWith("jar:file:")) {
            resolveJarClass(path);
        } else if (path.endsWith(".class")) {
            resolveClass(new File(path));
        } else if (path.endsWith(".jar")) {
            resolveJar(new File(path));
            cachePath.remove(path);
        } else {
            throw new RuntimeException("cache dir ?");
        }
    }

    private void loadFullClass(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    loadFullClass(childFile);
                }
            }
        } else if (file.getName().endsWith(".class")) {
            resolveClass(file);
        } else if (file.getName().endsWith(".jar")) {
            resolveJar(file);
        }
    }

    private void resolveClass(File file) throws IOException {
        count.incrementAndGet();
        FileInputStream stream = new FileInputStream(file);
        CtClass ctClass = pool.makeClass(stream);
        if (!TextUtil.exclude(ctClass.getName())) {
            if (classClassify.doClassify(ctClass)) {
                cachePath.add(file.getAbsolutePath());
            } else if (useCache) {
                cachePath.remove(file.getAbsolutePath());
            }
        }
        stream.close();
    }

    private void resolveJar(File file) throws IOException {
        JarFile jar = new JarFile(file);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            final JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                count.incrementAndGet();
                if (!TextUtil.exclude2(entry.getName())) {
                    InputStream stream = jar.getInputStream(entry);
                    String path = "jar:file:" + file.getAbsolutePath() + "!/" + entry.getName();
                    if (classClassify.doClassify(pool.makeClass(stream))) {
                        cachePath.add(path);
                    }
                    // no need to remove, as removed by handleJar
                    stream.close();
                }
            }
        }
    }

    private void resolveJarClass(String path) throws IOException {
        count.incrementAndGet();
        InputStream stream = new URL(path).openStream();
        if (!classClassify.doClassify(pool.makeClass(stream))) {
            cachePath.remove(path);
        }
        stream.close();
    }
    

}
