package com.didi.drouter.store;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.didi.drouter.service.IFeatureMatcher;

/**
 * Created by gaowei on 2019/1/31
 */
public class ServiceKey<T> {

    Class<T> function;
    @NonNull String alias = "";
    IFeatureMatcher<?> feature;
    int priority;
    Lifecycle lifecycle;
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

    public ServiceKey<T> setFeatureMatcher(IFeatureMatcher<?> feature) {
        this.feature = feature;
        return this;
    }

    public ServiceKey<T> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public ServiceKey<T> setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }
}
