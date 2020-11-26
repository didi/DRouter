package com.didi.drouter.store;

import android.arch.lifecycle.LifecycleOwner;
import android.support.annotation.NonNull;

import com.didi.drouter.service.IFeatureMatcher;

/**
 * Created by gaowei on 2019/1/31
 */
public class ServiceKey<T> {

    Class<T> function;
    @NonNull String alias = "";
    IFeatureMatcher<?> feature;
    LifecycleOwner lifecycleOwner;
    RouterMeta meta;

    private ServiceKey() {}

    public static <T> ServiceKey<T> build(Class<T> function) {
        ServiceKey<T> key = new ServiceKey<>();
        key.function = function;
        return key;
    }

    public ServiceKey<T> setAlias(String alias) {
        this.alias = alias != null ? alias : "";
        return this;
    }

    public ServiceKey<T> setFeature(IFeatureMatcher<?> feature) {
        this.feature = feature;
        return this;
    }

    public ServiceKey<T> setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        return this;
    }
}
