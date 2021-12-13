package com.didi.drouter.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.api.Extend;
import com.didi.drouter.remote.RemoteBridge;
import com.didi.drouter.store.IRouterProxy;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2019/4/1
 */
@SuppressWarnings("unchecked")
class ServiceAgent<T> {

    // cache, key is impl, without dynamic
    private static final Map<Class<?>, Object> sSingletonInstanceMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, WeakReference<Object>> sWeakInstanceMap = new ConcurrentHashMap<>();

    // all annotation and dynamic, sort by priority
    private final List<RouterMeta> orderMetaList = new ArrayList<>();
    private final Class<T> function;
    private @NonNull String alias = "";
    private Object feature;
    private String authority;
    private int resendStrategy;
    private WeakReference<LifecycleOwner> lifecycle;
    private @Nullable T defaultService;

    ServiceAgent(Class<T> function) {
        this.function = function;
        Set<RouterMeta> metaSet = RouterStore.getServiceMetas(function);
        for (RouterMeta meta : metaSet) {
            if (meta.getRouterType() == RouterMeta.SERVICE) {
                orderMetaList.add(meta);
            }
        }
        Collections.sort(orderMetaList, new ServiceComparator());
    }

    void setAlias(String alias) {
        this.alias = alias != null ? alias : "";
    }

    void setFeature(Object feature) {
        this.feature = feature;
    }

    void setRemoteAuthority(String authority) {
        this.authority = authority;
    }

    void setRemoteDeadResend(@Extend.Resend int strategy) {
        this.resendStrategy = strategy;
    }

    void setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycle = owner != null ? new WeakReference<>(owner) : null;
    }

    void setDefaultIfEmpty(T defaultService) {
        this.defaultService = defaultService;
    }

    // annotation class only, without dynamic
    @NonNull List<Class<? extends T>> getAllServiceClass() {
        List<Class<? extends T>> result = new ArrayList<>();
        if (function != null) {
            for (RouterMeta meta : orderMetaList) {
                if (!meta.isDynamic() && match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                    result.add((Class<? extends T>) meta.getRouterClass());
                }
            }
        }
        return result;
    }

    // annotation class only, without dynamic
    Class<? extends T> getServiceClass() {
        List<Class<? extends T>> result = getAllServiceClass();
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    @NonNull
    List<T> getAllService(Object... constructors) {
        List<T> result = new ArrayList<>();
        if (function != null) {
            for (RouterMeta meta : orderMetaList) {
                if (match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                    T t;
                    if (meta.isDynamic()) {
                        t = (T) meta.getDynamicService();
                    } else {
                        t = (T) getServiceInstance(meta, constructors);
                    }
                    if (t != null) {
                        result.add(t);
                    }
                }
            }
        }
        return result;
    }

    T getService(Object... constructors) {
        // remote
        if (!TextUtils.isEmpty(authority)) {
            return RemoteBridge.load(authority, resendStrategy, lifecycle)
                    .getService(function, alias, feature, constructors);
        }
        for (RouterMeta meta : orderMetaList) {
            if (match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                if (meta.isDynamic()) {
                    // dynamic class
                    RouterLogger.getCoreLogger().d("[Local] Get dynamic service \"%s\" with impl \"%s\"",
                            function.getSimpleName(), meta.getDynamicService().getClass().getName());
                    return (T) meta.getDynamicService();
                } else {
                    // annotation class
                    T target = (T) getServiceInstance(meta, constructors);
                    if (target != null) {
                        // ICallService
                        if (function == ICallService.class && CallHandler.isCallService(target)) {
                            RouterLogger.getCoreLogger().d("[Local] Get ICallService \"%s\" with impl \"%s\"",
                                    function.getSimpleName(), target.getClass().getSimpleName());
                            return (T) Proxy.newProxyInstance(
                                    getClass().getClassLoader(), new Class[]{function}, new CallHandler(target));
                        }
                        RouterLogger.getCoreLogger().d("[Local] Get service \"%s\" with impl \"%s\"",
                                function.getSimpleName(), target.getClass().getSimpleName());
                        return target;
                    }
                }
            }
        }
        RouterLogger.getCoreLogger().w("[Local] Get service \"%s\" fail with default \"%s\"",
                function.getSimpleName(), defaultService != null ? defaultService.getClass().getName() : null);
        return defaultService;
    }

    private boolean match(String alias, IFeatureMatcher<Object> feature) {
        return this.alias.equals(alias) && (feature == null || feature.match(this.feature));
    }

    @SuppressWarnings("ConstantConditions")
    private @Nullable Object getServiceInstance(RouterMeta meta, Object... parameter) {
        Class<?> implClass = meta.getRouterClass();
        if (implClass == null) {
            return null;
        }
        Object t = sSingletonInstanceMap.get(implClass);
        if (t == null && sWeakInstanceMap.containsKey(implClass)) {
            t = sWeakInstanceMap.get(implClass).get();
        }
        if (t == null) {
            synchronized (ServiceAgent.class) {
                t = sSingletonInstanceMap.get(implClass);
                if (t == null && sWeakInstanceMap.containsKey(implClass)) {
                    t = sWeakInstanceMap.get(implClass).get();
                }
                if (t == null) {
                    t = (parameter != null && parameter.length == 0) && meta.getRouterProxy() != null ?
                            meta.getRouterProxy().newInstance(null) : null;
                    if (t == null) {
                        t = ReflectUtil.getInstance(implClass, parameter);
                    }
                    if (t != null) {
                        if (meta.getCache() == Extend.Cache.SINGLETON) {
                            sSingletonInstanceMap.put(implClass, t);
                        } else if (meta.getCache() == Extend.Cache.WEAK) {
                            sWeakInstanceMap.put(implClass, new WeakReference<>(t));
                        }
                        return t;
                    }
                }
            }
        }
        return t;
    }

    // annotation class only
    IRouterProxy getRouterProxy(Class<?> implClass) {
        for (RouterMeta meta : orderMetaList) {
            if (meta.getRouterClass() == implClass) {
                return meta.getRouterProxy();
            }
        }
        return null;
    }

    private static class CallHandler implements InvocationHandler {

        Object callService;

        CallHandler(Object callService) {
            this.callService = callService;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object[] params = (Object[]) args[0];
            if (params == null) {
                params = new Object[]{null};
            }
            if (callService instanceof ICallService0 && params.length == 0) {
                return ((ICallService0) callService).call();
            }
            if (callService instanceof ICallService1 && params.length == 1) {
                return ((ICallService1) callService).call(params[0]);
            }
            if (callService instanceof ICallService2 && params.length == 2) {
                return ((ICallService2) callService).call(params[0], params[1]);
            }
            if (callService instanceof ICallService3 && params.length == 3) {
                return ((ICallService3) callService).call(params[0], params[1], params[2]);
            }
            if (callService instanceof ICallService4 && params.length == 4) {
                return ((ICallService4) callService).call(params[0], params[1], params[2], params[3]);
            }
            if (callService instanceof ICallService5 && params.length == 5) {
                return ((ICallService5) callService).call(params[0], params[1], params[2], params[3], params[4]);
            }
            if (callService instanceof ICallServiceN) {
                return ((ICallServiceN) callService).call(params);
            }
            RouterLogger.getCoreLogger().e("%s not match with argument length %s ",
                    callService.getClass().getSimpleName(), params.length);
            return null;
        }

        static boolean isCallService(Object instance) {
            return instance instanceof ICallService0 ||
                    instance instanceof ICallService1 ||
                    instance instanceof ICallService2 ||
                    instance instanceof ICallService3 ||
                    instance instanceof ICallService4 ||
                    instance instanceof ICallService5 ||
                    instance instanceof ICallServiceN;
        }
    }

    // from large to small
    private static class ServiceComparator implements Comparator<RouterMeta> {

        @Override
        public int compare(RouterMeta o1, RouterMeta o2) {
            int priority1 = o1.getPriority();
            int priority2 = o2.getPriority();
            return priority2 - priority1;
        }
    }
}
