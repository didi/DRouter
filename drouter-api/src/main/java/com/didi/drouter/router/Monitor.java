package com.didi.drouter.router;

import android.os.Handler;
import android.os.HandlerThread;

import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2019/1/17
 */
class Monitor {

    private static Handler timeoutHandler;

    static void startMonitor(final Request request, final Result result) {
        long period = request.holdTimeout;
        if (period > 0) {
            check();
            RouterLogger.getCoreLogger().d("monitor for request \"%s\" start, count down \"%sms\"",
                    request.getNumber(), period);
            timeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    RouterExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            ResultAgent.release(request, ResultAgent.STATE_TIMEOUT);
                        }
                    });
                }
            }, period);
        }
    }

    private static void check() {
        if (timeoutHandler == null) {
            synchronized (Monitor.class) {
                if (timeoutHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("timeout-monitor-thread");
                    handlerThread.start();
                    timeoutHandler = new Handler(handlerThread.getLooper());
                }
            }
        }
    }
}
