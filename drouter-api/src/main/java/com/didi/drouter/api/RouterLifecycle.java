package com.didi.drouter.api;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;

/**
 * Created by gaowei on 2020/9/26
 */
public class RouterLifecycle implements LifecycleOwner {
    
    private final LifecycleRegistry lifecycle = new LifecycleRegistry(this);

    public RouterLifecycle() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }
    
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void create() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }
    
    public void destroy() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }
}
