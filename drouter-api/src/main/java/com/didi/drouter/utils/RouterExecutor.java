package com.didi.drouter.utils;

import android.os.Handler;
import android.os.Looper;

import com.didi.drouter.api.Extend;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by gaowei on 2018/9/17
 */
public class RouterExecutor {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static ExecutorService threadPool = new RouterThreadExecutor(
            0, Integer.MAX_VALUE,
            60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

    public static void setThreadPool(ExecutorService threadPool) {
        RouterExecutor.threadPool = threadPool;
    }

    public static void execute(@Extend.Thread int mode, Runnable runnable) {
        switch (mode) {
            case Extend.Thread.MAIN:
                main(runnable);
                break;
            case Extend.Thread.WORKER:
                worker(runnable);
                break;
            case Extend.Thread.POSTING:
            default:
                runnable.run();
        }
    }

    public static void main(Runnable runnable) {
        main(runnable, 0);
    }

    public static void main(Runnable runnable, long timeDelay) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread() && timeDelay == 0) {
            runnable.run();
        } else {
            mainHandler.postDelayed(runnable, timeDelay);
        }
    }

    public static void worker(Runnable runnable) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()
                || Thread.currentThread().getName().contains("Binder")) {
            threadPool.submit(runnable);
        } else {
            runnable.run();
        }
    }

    public static void submit(Runnable runnable) {
        threadPool.submit(runnable);
    }

    public static void submit(Runnable runnable, long timeDelay) {
        mainHandler.postDelayed(() -> threadPool.submit(runnable), timeDelay);
    }

    static class RouterThreadExecutor extends ThreadPoolExecutor {

        public RouterThreadExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                    TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t == null && r instanceof Future<?> && ((Future<?>)r).isDone()) {
                try {
                    ((Future<?>) r).get();
                } catch (ExecutionException e) {
                    t = e.getCause();
                } catch (InterruptedException ignore) {
                }
            }
            if (t != null) {
                throw new RuntimeException(t);
            }
        }
    }

}
