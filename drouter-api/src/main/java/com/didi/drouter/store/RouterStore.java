package com.didi.drouter.store;

import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import com.didi.drouter.api.Extend;
import com.didi.drouter.loader.host.InterceptorLoader;
import com.didi.drouter.loader.host.RouterLoader;
import com.didi.drouter.loader.host.ServiceLoader;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;


/**
 * Created by gaowei on 2018/8/31
 */
@SuppressWarnings({"unchecked", "SpellCheckingInspection", "rawtypes"})
public class RouterStore {

    public static final String HOST = "host";
    static final String REGEX_ROUTER = "RegexRouter";

    // key is uriKey，value is meta, with dynamic
    // key is REGEX_ROUTER，value is map<uriKey, meta>
    private static final Map<String, Object> routerMetas = new ConcurrentHashMap<>();
    // key is interceptor impl or string name
    private static Map<Object, RouterMeta> interceptorMetas = new ConcurrentHashMap<>();
    // key is interface，value is set, with dynamic
    private static final Map<Class<?>, Set<RouterMeta>> serviceMetas = new ConcurrentHashMap<>();

    private static final Set<String> loadRecord = new CopyOnWriteArraySet<>();
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static volatile boolean initialized;

    /**
     * support VirtualApk
     *
     * @param app host or use {pluginName} configuration in plugin apk gradle file
     */
    public static void checkAndLoad(final String app, boolean async) {
        if (!loadRecord.contains(app)) {
            synchronized (RouterStore.class) {
                if (!loadRecord.contains(app)) {
                    loadRecord.add(app);
                    if (!async) {
                        Log.d(RouterLogger.CORE_TAG, "DRouter start load router table sync");
                        load(app);
                    } else {
                        new Thread("drouter-table-thread") {
                            @Override
                            public void run() {
                                Log.d(RouterLogger.CORE_TAG, "DRouter start load router table in drouter-table-thread");
                                load(app);
                            }
                        }.start();
                    }
                }
            }
        }
    }

    private static void load(String app) {
        long time = System.currentTimeMillis();
        boolean load;
        if (HOST.equals(app)) {
            load = loadHostTable();
            initialized = true;
            latch.countDown();
        } else {
            load = loadPluginTable(app,
                    Pair.create("Router", routerMetas),
                    Pair.create("Interceptor", interceptorMetas),
                    Pair.create("Service", serviceMetas));
        }
        if (!load) {
            RouterLogger.getCoreLogger().e(
                    "DRouterTable in app \"%s\" not found, " +
                            "please apply drouter plugin first.", app);
        }

        RouterLogger.getCoreLogger().d(
                "[===DRouter load complete=== waste time: %sms]",
                System.currentTimeMillis() - time);
    }

    private static boolean loadHostTable() {
        try {
            new RouterLoader().load(routerMetas);
            new InterceptorLoader().load(interceptorMetas);
            new ServiceLoader().load(serviceMetas);
        }  catch (NoClassDefFoundError e) {
            return false;
        }
        return true;
    }

    private static boolean loadPluginTable(String packageName, Pair... targets) {
        try {
            for (Pair<String, Map<?, ?>> target : targets) {
                MetaLoader loader = (MetaLoader) ReflectUtil.getInstance(
                        Class.forName(String.format("com.didi.drouter.loader.%s.%sLoader",
                                packageName, target.first))
                );
                assert loader != null;
                loader.load(target.second);
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    @NonNull
    public static Set<RouterMeta> getRouterMetas(@NonNull Uri uriKey) {
        check();
        Set<RouterMeta> result = new ArraySet<>();
        // exact match
        Object o = routerMetas.get(TextUtils.getStandardRouterKey(uriKey));
        if (o instanceof RouterMeta) {
            result.add((RouterMeta) o);
        }
        // fuzzy match include regex and placeholder
        Map<String, RouterMeta> regex = (Map<String, RouterMeta>) routerMetas.get(REGEX_ROUTER);
        if (regex != null) {
            for (RouterMeta meta : regex.values()) {
                if (meta.isRegexMatch(uriKey)) {
                    result.add(meta);
                }
            }
        }
        return result;
    }

    @NonNull
    public static Map<Object, RouterMeta> getInterceptors() {
        check();
        return interceptorMetas;
    }

    @NonNull
    public static Set<RouterMeta> getServiceMetas(Class<?> interfaceClass) {
        check();
        Set<RouterMeta> metas = serviceMetas.get(interfaceClass);
        if (metas == null) {
            return Collections.emptySet();
        }
        return metas;
    }

    @NonNull
    public synchronized static IRegister register(final RouterKey key, final IRouterHandler handler) {
        if (key == null || handler == null) {
            throw new IllegalArgumentException("argument null illegal error");
        }
        check();
        RouterMeta meta = RouterMeta.build(RouterMeta.HANDLER).assembleRouter(
                key.uri.getScheme(), key.uri.getHost(), key.uri.getPath(),
                (Class<?>) null, null, key.interceptor, key.interceptorName,
                key.thread, key.priority, key.hold);
        meta.setDynamicHandler(handler);
        boolean success = false;
        String standardRouterKey = TextUtils.getStandardRouterKey(key.uri);
        if (meta.isRegexUri()) {
            Map<String, RouterMeta> regexMap = (Map<String, RouterMeta>) routerMetas.get(REGEX_ROUTER);
            if (regexMap == null) {
                regexMap = new ConcurrentHashMap<>();
                routerMetas.put(REGEX_ROUTER, regexMap);
            }
            RouterMeta exist = regexMap.get(standardRouterKey);
            if (exist == null || exist.isDynamic()) {
                success = true;
                regexMap.put(standardRouterKey, meta);
            }
        } else {
            RouterMeta exist = (RouterMeta) routerMetas.get(standardRouterKey);
            if (exist == null || exist.isDynamic()) {
                success = true;
                routerMetas.put(standardRouterKey, meta);
            }
        }
        if (success && key.lifecycleOwner != null) {
            final WeakReference<Pair<RouterKey, IRouterHandler>> reference = new WeakReference<>(new Pair<>(key, handler));
            RouterExecutor.main(() -> key.lifecycleOwner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
                Pair<RouterKey, IRouterHandler> pair;
                if (event == Lifecycle.Event.ON_DESTROY && (pair = reference.get()) != null) {
                    unregister(pair.first, pair.second);
                }
            }));
        }
        RouterLogger.getCoreLogger().dw("register \"%s\" with handler \"%s\" %s",
                !success,
                TextUtils.getStandardRouterKey(key.uri),
                handler.getClass().getName(),
                success ? "success" : "fail");
        return new RouterRegister(key, handler);
    }

    synchronized static void unregister(RouterKey key, IRouterHandler handler) {
        if (key != null && handler != null) {
            RouterMeta meta = RouterMeta.build(RouterMeta.HANDLER).assembleRouter(
                    key.uri.getScheme(), key.uri.getHost(), key.uri.getPath(),
                    (Class<?>) null, null,
                    key.interceptor, key.interceptorName, key.thread, key.priority, key.hold);
            boolean success = false;
            if (meta.isRegexUri()) {
                Map<String, RouterMeta> regexMap = (Map<String, RouterMeta>) routerMetas.get(REGEX_ROUTER);
                if (regexMap != null) {
                    RouterMeta curMeta = regexMap.get(TextUtils.getStandardRouterKey(key.uri));
                    if (curMeta != null && curMeta.getDynamicHandler() == handler) {
                        success = regexMap.remove(TextUtils.getStandardRouterKey(key.uri)) != null;
                    }
                }
            } else {
                RouterMeta curMeta = (RouterMeta) routerMetas.get(TextUtils.getStandardRouterKey(key.uri));
                if (curMeta != null && curMeta.getDynamicHandler() == handler) {
                    success = routerMetas.remove(TextUtils.getStandardRouterKey(key.uri)) != null;
                }
            }
            if (success) {
                RouterLogger.getCoreLogger().d("unregister \"%s\" with handler \"%s\" success",
                        TextUtils.getStandardRouterKey(key.uri), handler.getClass().getName());
            }
        }
    }

    @NonNull
    public synchronized static <T> IRegister register(final ServiceKey<T> key, final T service) {
        if (key == null || key.function == null || service == null) {
            throw new IllegalArgumentException("argument null illegal error");
        }
        check();
        RouterMeta meta = RouterMeta.build(RouterMeta.SERVICE).assembleService(
                null, null,
                key.alias, key.feature, key.priority, Extend.Cache.NO);
        meta.setDynamicService(service);
        key.meta = meta;
        Set<RouterMeta> metas = serviceMetas.get(key.function);
        if (metas == null) {
            metas = Collections.newSetFromMap(new ConcurrentHashMap<>());
            serviceMetas.put(key.function, metas);
        }
        metas.add(meta);
        if (key.lifecycleOwner != null) {
            final WeakReference<Pair<ServiceKey<T>, T>> reference = new WeakReference<>(new Pair<>(key, service));
            RouterExecutor.main(() -> key.lifecycleOwner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
                Pair<ServiceKey<T>, T> pair;
                if (event == Lifecycle.Event.ON_DESTROY && (pair = reference.get()) != null) {
                    unregister(pair.first, pair.second);
                }
            }));
        }
        RouterLogger.getCoreLogger().d("register \"%s\" with service \"%s\" success, size:%s",
                key.function.getName(), service, metas.size());
        return new RouterRegister(key, service);
    }

    synchronized static void unregister(ServiceKey<?> key, Object service) {
        if (key != null && service != null) {
            Set<RouterMeta> metas = serviceMetas.get(key.function);
            if (metas != null) {
                if (metas.remove(key.meta)) {
                    RouterLogger.getCoreLogger().d("unregister \"%s\" with service \"%s\" success",
                            key.function.getName(), service);
                }
            }
        }
    }

    private static void check() {
        if (!initialized) {
            RouterStore.checkAndLoad(HOST, false);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}