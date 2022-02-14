package com.didi.drouter.service;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.remote.Strategy;

import java.util.List;

/**
 * Created by gaowei on 2018/9/4
 */
public class ServiceLoader<T> {

    final ServiceAgent<T> serviceAgent;

    private ServiceLoader(Class<T> function) {
        serviceAgent = new ServiceAgent<>(function);
    }

    public static @NonNull <T> ServiceLoader<T> build(Class<T> function) {
        if (function == null) throw new RuntimeException("DRouter function class can't be null");
        return new ServiceLoader<>(function);
    }

    public ServiceLoader<T> setAlias(String alias) {
        serviceAgent.setAlias(alias);
        return this;
    }

    public ServiceLoader<T> setFeature(Object feature) {
        serviceAgent.setFeature(feature);
        return this;
    }

    public ServiceLoader<T> setRemote(Strategy strategy) {
        serviceAgent.setRemote(strategy);
        return this;
    }

    /**
     * If set, it will auto stop resend behavior when lifecycle is destroy
     * {@link Strategy#setResend}
     */
    public ServiceLoader<T> setLifecycleOwner(LifecycleOwner owner) {
        serviceAgent.setLifecycleOwner(owner);
        return this;
    }

    /**
     * When there is no service return from {@link ServiceLoader#getService(Object...)} with no remote,
     * this object can be returned by default.
     */
    public ServiceLoader<T> setDefaultIfEmpty(T defaultService) {
        serviceAgent.setDefaultIfEmpty(defaultService);
        return this;
    }

    /**
     * Remote process support.
     * When executed locally, null may be returned, but not if executed through remote process.
     */
    public T getService(Object... parameter) {
        return serviceAgent.getService(parameter);
    }

    public @NonNull List<T> getAllService(Object... parameter) {
        return serviceAgent.getAllService(parameter);
    }

    public Class<? extends T> getServiceClass() {
        return serviceAgent.getServiceClass();
    }

    public @NonNull List<Class<? extends T>> getAllServiceClass() {
        return serviceAgent.getAllServiceClass();
    }
}