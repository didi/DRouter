package com.didi.drouter.utils;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RestrictTo;

import com.didi.drouter.api.Extend;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by gaowei on 2018/9/17
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RouterExecutor {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

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
        if (java.lang.Thread.currentThread() == Looper.getMainLooper().getThread() && timeDelay == 0) {
            runnable.run();
        } else {
            mainHandler.postDelayed(runnable, timeDelay);
        }
    }

    public static void worker(Runnable runnable) {
        if (java.lang.Thread.currentThread() == Looper.getMainLooper().getThread()) {
            threadPool.submit(runnable);
        } else {
            runnable.run();
        }
    }

    public static void submit(Runnable runnable) {
        threadPool.submit(runnable);
    }

}
