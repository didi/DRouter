package com.didi.drouter.service;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
@SuppressWarnings({"unchecked"})
class ServiceAgent<T> {

    // cache, key is impl
    private static final Map<Class<?>, Object> sInstanceMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, WeakReference<Object>> sWeakInstanceMap = new ConcurrentHashMap<>();

    // key is impl, normal class only
    private final Map<Class<?>, RouterMeta> routerClassImplMap = new ConcurrentHashMap<>();
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
            if (meta.getRouterType() == RouterMeta.SERVICE && !meta.isDynamic()) {
                routerClassImplMap.put(meta.getRouterClass(), meta);
            }
        }
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

    // normal class only
    @NonNull List<Class<? extends T>> getAllServiceClass() {
        List<Class<? extends T>> result = new ArrayList<>();
        if (function != null) {
            for (RouterMeta meta : routerClassImplMap.values()) {
                if (!meta.isDynamic() && match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                    result.add((Class<? extends T>) meta.getRouterClass());
                }
            }
            if (result.size() > 1) {
                Collections.sort(result, new ServiceComparator());
            }
        }
        return result;
    }

    // normal class only
    Class<? extends T> getServiceClass() {
        List<Class<? extends T>> result = getAllServiceClass();
        if (!result.isEmpty()) {
            return result.get(0);
        }
        return null;
    }

    // all and dynamic first
    @NonNull
    List<T> getAllService(Object... constructors) {
        List<T> result = new ArrayList<>();
        if (function != null) {
            for (RouterMeta meta : RouterStore.getServiceMetas(function)) {
                if (meta.isDynamic() && match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                    result.add((T) meta.getService());
                }
            }
            for (Class<? extends T> clz : getAllServiceClass()) {
                T t = (T) getServiceInstance(clz, constructors);
                if (t != null) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    T getService(Object... constructors) {
        // remote
        if (!TextUtils.isEmpty(authority)) {
            RouterLogger.getCoreLogger().d(
                    "[..] Get remote service \"%s\" by RemoteBridge", function.getSimpleName());
            return RemoteBridge.load(authority, resendStrategy, lifecycle)
                    .getService(function, alias, feature, constructors);
        }
        // dynamic class
        for (RouterMeta meta : RouterStore.getServiceMetas(function)) {
            if (meta.isDynamic() && match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                RouterLogger.getCoreLogger().d("[..] Get local dynamic service \"%s\" with result \"%s\"",
                        function.getSimpleName(), meta.getService().getClass().getName());
                return (T) meta.getService();
            }
        }
        // normal class
        T target = (T) getServiceInstance(getServiceClass(), constructors);
        if (target != null) {
            // ICallServiceX
            if (function == ICallService.class && CallHandler.isCallService(target)) {
                RouterLogger.getCoreLogger().d("[..] Get local ICallService service \"%s\" with result \"%s\"",
                        function.getSimpleName(), target.getClass().getSimpleName());
                return (T) Proxy.newProxyInstance(
                        getClass().getClassLoader(), new Class[]{function}, new CallHandler(target));
            }
            RouterLogger.getCoreLogger().d("[..] Get local normal service \"%s\" with result \"%s\"",
                    function.getSimpleName(), target.getClass().getSimpleName());
            return (T) target;
        }
        RouterLogger.getCoreLogger().w("[..] Get local service \"%s\" fail with default instance \"%s\"",
                function.getSimpleName(), defaultService != null ? defaultService.getClass().getName() : null);
        return defaultService;
    }

    private boolean match(String alias, IFeatureMatcher<Object> feature) {
        return this.alias.equals(alias) && (feature == null || feature.match(this.feature));
    }

    private @Nullable Object getServiceInstance(Class<?> implClass, Object... parameter) {
        if (implClass == null) {
            return null;
        }
        Object t = sInstanceMap.get(implClass);
        if (t == null && sWeakInstanceMap.containsKey(implClass)) {
            t = sWeakInstanceMap.get(implClass).get();
        }
        if (t == null) {
            synchronized (ServiceAgent.class) {
                t = sInstanceMap.get(implClass);
                if (t == null && sWeakInstanceMap.containsKey(implClass)) {
                    t = sWeakInstanceMap.get(implClass).get();
                }
                if (t == null) {
                    RouterMeta meta = routerClassImplMap.get(implClass);
                    t = (parameter != null && parameter.length == 0) && meta.getRouterProxy() != null ?
                            meta.getRouterProxy().newInstance(null) : null;
                    if (t == null) {
                        t = ReflectUtil.getInstance(implClass, parameter);
                    }
                    if (t != null) {
                        RouterLogger.getCoreLogger().d("[..] Create new service \"%s\" instance success",
                                t.getClass().getSimpleName());
                        if (routerClassImplMap.get(implClass).getCache() == Extend.Cache.SINGLETON) {
                            sInstanceMap.put(implClass, t);
                        } else if (routerClassImplMap.get(implClass).getCache() == Extend.Cache.WEAK) {
                            sWeakInstanceMap.put(implClass, new WeakReference<>(t));
                        }
                        return t;
                    }
                }
            }
        }
        if (t != null) {
            RouterLogger.getCoreLogger().d("[..] Get service \"%s\" instance by cache",
                    t.getClass().getSimpleName());
        }
        return t;
    }

    IRouterProxy getRouterProxy(Class<?> implClass) {
        RouterMeta meta = routerClassImplMap.get(implClass);
        if (meta != null) {
            return meta.getRouterProxy();
        }
        return null;
    }

    private static class CallHandler implements InvocationHandler {

        Object callService;

        CallHandler(Object callService) {
            this.callService = callService;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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

    // from large to small, normal
    private class ServiceComparator implements Comparator<Class<?>> {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            int priority1 = routerClassImplMap.get(o1).getPriority();
            int priority2 = routerClassImplMap.get(o2).getPriority();
            return priority2 - priority1;
        }
    }
}
