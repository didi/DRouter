package com.didi.drouter.api;

import android.app.Application;
import android.support.annotation.NonNull;

import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.router.Request;
import com.didi.drouter.store.RouterKey;
import com.didi.drouter.store.ServiceKey;
import com.didi.drouter.service.ServiceLoader;
import com.didi.drouter.store.IRegister;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.SystemUtil;

/**
 * Created by gaowei on 2018/8/31
 */
public class DRouter {

    public static Application getContext() {
        return SystemUtil.getApplication();
    }

    /**
     * If main process, it will be executed automatically.
     * You can also execute it manually by your self or in worker thread.
     * @param application Application
     */
    public static void init(Application application) {
        SystemUtil.setApplication(application);
        RouterStore.checkAndLoad(RouterStore.HOST, true);
    }

    /**
     * Navigation to router
     * there will be only one activity match at most, but may be several router handler.
     * @param uri String
     * @return request
     */
    @NonNull
    public static Request build(String uri) {
        return Request.build(uri);
    }

    /**
     * Navigation to service
     * @param function service interface in service annotation
     * @return ServiceLoader
     */
    @NonNull
    public static <T> ServiceLoader<T> build(Class<T> function) {
        return ServiceLoader.build(function);
    }

    /**
     * Register dynamic handler
     * @param key routerKey
     * @param handler handler instance
     */
    @NonNull
    public static IRegister register(RouterKey key, IRouterHandler handler) {
        return RouterStore.register(key, handler);
    }

    /**
     * Register dynamic service
     * @param key serviceKey
     * @param service service instance
     */
    @NonNull
    public static <T> IRegister register(ServiceKey<T> key, T service) {
        return RouterStore.register(key, service);
    }

}
