package com.didi.drouter.router;

import static com.didi.drouter.router.Result.INTERCEPT;

import androidx.annotation.NonNull;

import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.RouterLogger;

import java.util.Queue;

/**
 * Created by gaowei on 2018/9/6
 */
class InterceptorHandler {

    static void handleGlobal(Request request, IRouterInterceptor.IInterceptor callback) {
        RouterLogger.getCoreLogger().d(">> Enter request \"%s\" (global) interceptors", request.getNumber());
        Queue<IRouterInterceptor> interceptors = InterceptorLoader.loadGlobal();
        handleNext(interceptors, request, callback);
    }

    // handle all related interceptor, recursion
    static void handleRelated(Request request,
                              RouterMeta meta,
                              IRouterInterceptor.IInterceptor callback) {
        RouterLogger.getCoreLogger().d(">> Enter request \"%s\" (related) interceptors", request.getNumber());
        Queue<IRouterInterceptor> interceptors = InterceptorLoader.loadRelated(meta);
        handleNext(interceptors, request, callback);
    }

    private static void handleNext(@NonNull final Queue<IRouterInterceptor> interceptors,
                                   final Request request,
                                   final IRouterInterceptor.IInterceptor callback) {
        final IRouterInterceptor interceptor = interceptors.poll();
        if (interceptor == null) {
            RouterLogger.getCoreLogger().d("<< Pass request \"%s\" interceptors", request.getNumber());
            callback.onContinue();
            return;
        }

        RouterMeta interceptorMeta = RouterStore.getInterceptors().get(interceptor.getClass());
        assert interceptorMeta != null;
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
                onInterrupt(INTERCEPT);
            }

            @Override
            public void onInterrupt(int statusCode) {
                RouterLogger.getCoreLogger().w("request \"%s\" interrupt by \"%s\"",
                        request.getNumber(), interceptor.getClass().getSimpleName());
                callback.onInterrupt(statusCode);
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

        @Override
        public void onInterrupt(int statusCode) {

        }
    }

}
