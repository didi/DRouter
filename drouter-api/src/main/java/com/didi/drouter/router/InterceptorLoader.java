package com.didi.drouter.router;

import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;

import com.didi.drouter.api.Extend;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.ReflectUtil;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Created by gaowei on 2018/9/5
 */
class InterceptorLoader {

    private static final Map<Class<? extends IRouterInterceptor>, IRouterInterceptor>
            sStaticInstanceMap = new ArrayMap<>();
    private static final Map<Class<? extends IRouterInterceptor>, WeakReference<IRouterInterceptor>>
            sWeakInstanceMap = new ArrayMap<>();
    private static final Set<Class<? extends IRouterInterceptor>>
            globalInterceptor = new ArraySet<>();

    static {
        for (Map.Entry<Class<? extends IRouterInterceptor>, RouterMeta> entry :
                RouterStore.getInterceptors().entrySet()) {
            if (entry.getValue().isGlobal()) {
                globalInterceptor.add(entry.getKey());
            }
        }
    }

    static @NonNull Queue<IRouterInterceptor> load(@NonNull RouterMeta meta) {
        Set<Class<? extends IRouterInterceptor>> allInterceptors = new ArraySet<>(globalInterceptor);
        Class<? extends IRouterInterceptor>[] interceptors = meta.getInterceptors();
        if (interceptors != null) {
            allInterceptors.addAll(Arrays.asList(interceptors));
        }
        Queue<IRouterInterceptor> result = new PriorityQueue<>(11, new InterceptorComparator());
        for (Class<? extends IRouterInterceptor> interceptorClass : allInterceptors) {
            result.add(getInstance(interceptorClass));
        }
        return result;
    }

    // from large to small
    private static class InterceptorComparator implements Comparator<IRouterInterceptor> {
        @Override
        public int compare(IRouterInterceptor o1, IRouterInterceptor o2) {
            int priority1 = RouterStore.getInterceptors().get(o1.getClass()).getPriority();
            int priority2 = RouterStore.getInterceptors().get(o2.getClass()).getPriority();
            return priority2 - priority1;
        }
    }

    private static IRouterInterceptor getInstance(Class<? extends IRouterInterceptor> clz) {
        IRouterInterceptor t = sStaticInstanceMap.get(clz);
        if (t == null && sWeakInstanceMap.containsKey(clz)) {
            t = sWeakInstanceMap.get(clz).get();
        }
        if (t == null) {
            synchronized (InterceptorLoader.class) {
                t = sStaticInstanceMap.get(clz);
                if (t == null && sWeakInstanceMap.containsKey(clz)) {
                    t = sWeakInstanceMap.get(clz).get();
                }
                if (t == null) {
                    RouterMeta meta = RouterStore.getInterceptors().get(clz);
                    if (meta == null) {
                        // for priority
                        meta = RouterMeta.build(RouterMeta.INTERCEPTOR)
                                .assembleInterceptor(clz, null, 0, false, Extend.Cache.NO);
                        RouterStore.getInterceptors().put(clz, meta);
                    }
                    t = meta.getRouterProxy() != null ?
                            (IRouterInterceptor) meta.getRouterProxy().newInstance(null) : null;
                    if (t == null) {
                        t = (IRouterInterceptor) ReflectUtil.getInstance(clz);
                    }
                    if (meta.getCache() == Extend.Cache.SINGLETON) {
                        sStaticInstanceMap.put(clz, t);
                    } else if (meta.getCache() == Extend.Cache.WEAK) {
                        sWeakInstanceMap.put(clz, new WeakReference<>(t));
                    }
                }
            }
        }
        return t;
    }

}
