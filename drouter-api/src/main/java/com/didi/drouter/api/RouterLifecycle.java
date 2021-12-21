package com.didi.drouter.api;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.support.annotation.NonNull;

/**
 * Created by gaowei on 2020/9/26
 *
 * RouterLifecycle can be used to set resend switch.
 * When the state is create resend mode is turned on and when the state is destroy resend mode is turned off
 * {@link com.didi.drouter.api.Extend.Resend}
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

    /**
     * Turn on resend switch, and it can be created repeatedly after being destroyed.
     */
    public void create() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }

    /**
     * Turn off resend switch
     */
    public void destroy() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }
}
