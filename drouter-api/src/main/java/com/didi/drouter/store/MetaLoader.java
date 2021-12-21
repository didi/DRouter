package com.didi.drouter.store;

import android.support.annotation.RestrictTo;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2018/8/30
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class MetaLoader {

    public abstract void load(Map<?, ?> data);

    // for regex router
    protected void put(String uri, RouterMeta meta, Map<String, Map<String, RouterMeta>> data) {
        Map<String, RouterMeta> map = data.get(RouterStore.REGEX_ROUTER);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            data.put(RouterStore.REGEX_ROUTER, map);
        }
        map.put(uri, meta);
    }

    // for service
    protected void put(Class<?> clz, RouterMeta meta, Map<Class<?>, Set<RouterMeta>> data) {
        Set<RouterMeta> set = data.get(clz);
        if (set == null) {
            set = Collections.newSetFromMap(new ConcurrentHashMap<RouterMeta, Boolean>());
            data.put(clz, set);
        }
        set.add(meta);
    }
}
