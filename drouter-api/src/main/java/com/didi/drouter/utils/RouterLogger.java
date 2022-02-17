package com.didi.drouter.utils;

import android.util.Log;
import android.widget.Toast;

import com.didi.drouter.api.DRouter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2018/9/6
 */
public class RouterLogger {

    public static final String CORE_TAG = "DRouterCore";
    public static final String APP_TAG = "DRouterAPP";
    private static boolean enable = false;
    private static ILogPrinter printer = new InnerLogPrinter();
    private static final RouterLogger coreLogger = new RouterLogger(CORE_TAG);
    private static RouterLogger appLogger;

    private final String tag;
    private static Map<String, Long> time;

    private RouterLogger(String tag) {
        this.tag = tag;
    }

    public static void setEnable(boolean enable) {
        RouterLogger.enable = enable;
    }

    public static void setPrinter(ILogPrinter logger) {
        RouterLogger.printer = logger;
    }

    public static RouterLogger getAppLogger() {
        if (appLogger == null) {
            synchronized (RouterLogger.class) {
                if (appLogger == null) {
                    appLogger = new RouterLogger(APP_TAG);
                }
            }
        }
        return appLogger;
    }

    public static RouterLogger getCoreLogger() {
        return coreLogger;
    }

    public void d(String content, Object... args) {
        if (content != null && isPrint()) {
            printer.d(tag, format(content, args));
        }
    }

    public void w(String content, Object... args) {
        if (content != null && isPrint()) {
            printer.w(tag, format(content, args));
        }
    }

    public void e(String content, Object... args) {
        if (content != null && isPrint()) {
            printer.e(tag, format(content, args));
        }
    }

    public static void t1(String tag) {
        if (time == null) {
            synchronized (RouterLogger.class) {
                if (time == null) {
                    time = new ConcurrentHashMap<>();
                }
            }
        }
        time.put(tag, System.currentTimeMillis());
    }

    public static void t2(String tag) {
        Long last = time.remove(tag);
        if (last != null) {
            getCoreLogger().d("RouterTimeTag:\"%s\" =>time:%s", tag, System.currentTimeMillis() - last);
        }
    }

    public void dw(String content, boolean isWarn, Object... args) {
        if (content != null && isPrint()) {
            if (isWarn) {
                printer.w(tag, format(content, args));
            } else {
                printer.d(tag, format(content, args));
            }
        }
    }

    public void de(String content, boolean isError, Object... args) {
        if (content != null && isPrint()) {
            if (isError) {
                printer.e(tag, format(content, args));
            } else {
                printer.d(tag, format(content, args));
            }
        }
    }

    public void crash(String content, Object... args) {
        if (content != null && isPrint()) {
            printer.e(tag, format(content, args) +
                    "\n Exception:" + Log.getStackTraceString(new Throwable()));
        }
        throw new RuntimeException(content);
    }

    public static void toast(final String string, final Object... args) {
        RouterExecutor.main(() -> Toast.makeText(DRouter.getContext(), format(string, args), Toast.LENGTH_SHORT).show());
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

    private static boolean isPrint() {
        return (SystemUtil.isDebug || enable) && printer != null;
    }

    public interface ILogPrinter {

        void d(String TAG, String content);
        void w(String TAG, String content);
        void e(String TAG, String content);
    }

    private static class InnerLogPrinter implements ILogPrinter {

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
