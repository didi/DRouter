package com.didi.drouter.router;

import android.support.annotation.NonNull;

import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.RouterLogger;

import java.util.Queue;

/**
 * Created by gaowei on 2018/9/6
 */
class InterceptorHandler {

    static void handle(final Request request,
                              RouterMeta meta,
                              final IRouterInterceptor.IInterceptor callback) {
        RouterLogger.getCoreLogger().d(">> Enter request \"%s\" all interceptors", request.getNumber());
        Queue<IRouterInterceptor> interceptors = InterceptorLoader.load(meta);
        handleNext(interceptors, request, callback);
    }

    private static void handleNext(@NonNull final Queue<IRouterInterceptor> interceptors,
                                   final Request request,
                                   final IRouterInterceptor.IInterceptor callback) {
        final IRouterInterceptor interceptor = interceptors.poll();
        if (interceptor == null) {
            RouterLogger.getCoreLogger().d("<< Pass request \"%s\" all interceptors", request.getNumber());
            callback.onContinue();
            return;
        }

        RouterMeta interceptorMeta = RouterStore.getInterceptors().get(interceptor.getClass());
        RouterLogger.getCoreLogger().d(
                "interceptor \"%s\" execute, for request \"%s\", global:%s, priority:%s",
                interceptor.getClass().getSimpleName(),
                request.getNumber(),
                interceptorMeta.isGlobal(),
                interceptorMeta.getPriority());
        request.interceptor = new IRouterInterceptor.IInterceptor() {

            @Override
            public void onContinue() {
                handleNext(interceptors, request, callback);
            }

            @Override
            public void onInterrupt() {
                RouterLogger.getCoreLogger().w("request \"%s\" interrupt by \"%s\"",
                        request.getNumber(), interceptor.getClass().getSimpleName());
                callback.onInterrupt();
            }
        };
        interceptor.handle(request);
    }

    static class Default implements IRouterInterceptor.IInterceptor {

        @Override
        public void onContinue() {

        }

        @Override
        public void onInterrupt() {

        }
    }

}
