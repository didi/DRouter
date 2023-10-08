package com.didi.drouter.plugin;

import com.didi.drouter.generator.ClassClassify;
import com.didi.drouter.utils.JarUtils;
import com.didi.drouter.utils.Logger;
import com.didi.drouter.utils.StoreUtil;
import com.didi.drouter.utils.SystemUtil;
import com.didi.drouter.utils.TextUtil;

import org.apache.commons.io.FileUtils;
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
    private final Queue<File> compileClassPath;
    private final Set<String> cachePathSet; //.class | .jar | .jar!/class
    private final File wTmpDir;
    private final boolean useCache;
    private final File routerDir;

    private ClassPool pool;
    private ClassClassify classClassify;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private final AtomicInteger count = new AtomicInteger();

    public RouterTask(Queue<File> compileClassPath, Set<String> cachePathSet, boolean useCache, File routerDir) {
        this.compileClassPath = compileClassPath;
        this.cachePathSet = cachePathSet;
        this.useCache = useCache;
        this.routerDir = routerDir;
        this.wTmpDir = SystemUtil.INSTANCE.isWindow() ? new File(SystemUtil.cacheDir, String.valueOf(System.currentTimeMillis())) : null;
    }

    public void run() {
        StoreUtil.clear();
        JarUtils.printVersion(project, compileClassPath);
        // https://github.com/didi/DRouter/issues/56
        if (wTmpDir != null) {
            ClassPool.cacheOpenedJarFile = false;
        }
        pool = new ClassPool();
        classClassify = new ClassClassify(pool, SystemUtil.setting);
        startExecute();
    }

    private void startExecute() {
        try {
            long timeStart = System.currentTimeMillis();
            for (File file : compileClassPath) {
                pool.appendClassPath(file.getAbsolutePath());
            }
            if (useCache) {
                loadCachePaths(cachePathSet);
            } else {
                loadFullPaths(compileClassPath);
            }
            Logger.d("load class used: " + (System.currentTimeMillis() - timeStart) + "ms");
            timeStart = System.currentTimeMillis();
            classClassify.generatorRouter(routerDir);
            Logger.d("generator router table used: " + (System.currentTimeMillis() - timeStart) + "ms");
            Logger.v("scan class size: " + count.get() + " | router class size: " + cachePathSet.size());
        } catch (Exception e) {
            JarUtils.INSTANCE.check(e);
            throw new GradleException("Could not generate d_router table\n" + e.getMessage(), e);
        } finally {
            executor.shutdown();
            FileUtils.deleteQuietly(wTmpDir);
        }
    }

    private void loadCachePaths(final Set<String> cachePath) throws IOException {
        // multi-thread for entry class of same jar may conflict
        Logger.d("start load cache with incremental:");
        for (String path : cachePath) {
            Logger.d("  path: " + path);
            loadCachePath(path);
        }
    }

    private void loadFullPaths(final Queue<File> files) throws ExecutionException, InterruptedException, IOException {
        if (!RouterSetting.Parse.debug) {
            final List<Future<Void>> taskList = new ArrayList<>();
            for (int i = 0; i < CPU_COUNT; i++) {
                taskList.add(executor.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        File file;
                        while ((file = files.poll()) != null) {
                            loadFullPath(file);
                        }
                        return null;
                    }
                }));
            }
            for (Future<Void> task : taskList) {
                task.get();
            }
        } else {
            Logger.d("start load full:");
            for (File file : files) {
                Logger.d("  path: " + file.getAbsolutePath());
                loadFullPath(file);
            }
        }
    }

    private void loadCachePath(String path) throws IOException {
        if (path.startsWith("jar:file:")) {
            // class in jar file
            resolveCachedClassInJar(path);
        } else if (path.endsWith(".class")) {
            // class file
            resolveClassFile(new File(path));
        } else if (path.endsWith(".jar")) {
            // jar file added in transform
            resolveJarFile(new File(path));
            cachePathSet.remove(path);
        } else {
            throw new RuntimeException("is cached dir ?");
        }
    }

    private void loadFullPath(File file) throws IOException {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    loadFullPath(childFile);
                }
            }
        } else if (file.getName().endsWith(".class")) {
            resolveClassFile(file);
        } else if (file.getName().endsWith(".jar")) {
            resolveJarFile(file);
        }
    }

    private void resolveClassFile(File file) throws IOException {
        count.incrementAndGet();
        FileInputStream stream = new FileInputStream(createFile(file, ".class"));
        CtClass ctClass;
        try {
            ctClass = pool.makeClass(stream);
        } catch (Exception e) {
            Logger.w("drouter resolve class error," +
                    " file=" + file.getAbsolutePath() +
                    " exception=" + e.getMessage());
            return;
        } finally {
            stream.close();
        }
        if (!TextUtil.excludePackageClass(ctClass.getName())) {
            if (classClassify.doClassify(ctClass)) {
                cachePathSet.add(file.getAbsolutePath());
            } else if (useCache) {
                cachePathSet.remove(file.getAbsolutePath());
            }
        }
    }

    private void resolveJarFile(File file) throws IOException {
        if (!TextUtil.excludeJarNameFile(file.getName())) {
            JarFile jar = new JarFile(createFile(file, ".jar"));
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    count.incrementAndGet();
                    if (!TextUtil.excludePackageClassInJar(entry.getName())) {
                        try (InputStream stream = jar.getInputStream(entry)) {
                            String path = "jar:file:" + file.getAbsolutePath() + "!/" + entry.getName();
                            CtClass clz = pool.makeClass(stream);
                            if (classClassify.doClassify(clz)) {
                                cachePathSet.add(path);
                            } else if (useCache) {
                                cachePathSet.remove(path);
                            }
                        } catch (Exception e) {
                            Logger.w("drouter resolve jar class error," +
                                    " jar=" + file.getAbsolutePath() +
                                    " entry=" + entry.getName() +
                                    " exception=" + e.getMessage());
                            throw e;
                        }
                    }
                }
            }
            jar.close();
        }
    }

    private void resolveCachedClassInJar(String path) throws IOException {
        count.incrementAndGet();
        try (InputStream stream = new URL(path).openStream()) {
            CtClass ctClass = pool.makeClass(stream);
            if (!classClassify.doClassify(ctClass)) {
                cachePathSet.remove(path);
            }
        } catch (Exception e) {
            Logger.e("drouter resolve jar class error," +
                    " entry=" + path +
                    " exception=" + e.getMessage());
            throw e;
        }
    }

    private File createFile(File file, String suffix) throws IOException {
        if (wTmpDir != null) {
            File wFile = new File(wTmpDir, String.format("%s-%s%s",
                    Thread.currentThread().getId(),
                    System.currentTimeMillis(),
                    suffix));
            FileUtils.copyFile(file, wFile);
            return wFile;
        }
        return file;
    }


}
