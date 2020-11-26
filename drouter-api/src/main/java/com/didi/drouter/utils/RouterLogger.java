package com.didi.drouter.utils;

import android.util.Log;
import android.widget.Toast;

import com.didi.drouter.api.DRouter;

/**
 * Created by gaowei on 2018/9/6
 */
public class RouterLogger {

    public static final String NAME = "DRouterCore";
    private static IRouterLogger logger = new InnerLogger();
    private static final RouterLogger coreLogger = new RouterLogger(NAME);
    private static final RouterLogger appLogger = new RouterLogger("DRouterApp");

    private final String tag;

    private RouterLogger(String tag) {
        this.tag = tag;
    }

    public static void setLogger(IRouterLogger logger) {
        RouterLogger.logger = logger;
    }

    public static RouterLogger getAppLogger() {
        return appLogger;
    }

    public static RouterLogger getCoreLogger() {
        return coreLogger;
    }

    public void d(String content, Object... args) {
        if (content != null && logger != null) {
            logger.d(tag, format(content, args));
        }
    }

    public void w(String content, Object... args) {
        if (content != null && logger != null) {
            logger.w(tag, format(content, args));
        }
    }

    public void e(String content, Object... args) {
        if (content != null && logger != null) {
            logger.e(tag, format(content, args));
        }
    }

    public void crash(String content, Object... args) {
        if (content != null && logger != null) {
            logger.e(tag, format(content, args) +
                    "\n Exception:" + Log.getStackTraceString(new Throwable()));
        }
        throw new RuntimeException(content);
    }

    public static void toast(final String string, final Object... args) {
        RouterExecutor.main(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DRouter.getContext(), format(string, args), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String format(String s, Object... args) {
        if (args == null) return s;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Throwable) {
                args[i] = Log.getStackTraceString((Throwable) args[i]);
            }
        }
        return String.format(s, args);
    }

    private static class InnerLogger implements IRouterLogger {

        @Override
        public void d(String tag, String content) {
            Log.d(tag, content);
        }

        @Override
        public void w(String tag, String content) {
            Log.w(tag, content);
        }

        @Override
        public void e(String tag, String content) {
            Log.e(tag, content);
        }
    }
}
