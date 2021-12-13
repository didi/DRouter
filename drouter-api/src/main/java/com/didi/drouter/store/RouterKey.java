package com.didi.drouter.store;

import android.net.Uri;

import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.api.Extend;
import com.didi.drouter.router.IRouterInterceptor;

/**
 * Created by gaowei on 2019/1/31
 */
public class RouterKey {

    Uri uri;
    Class<? extends IRouterInterceptor>[] interceptor;
    String[] interceptorName;
    int thread;
    boolean hold;
    int priority;
    LifecycleOwner lifecycleOwner;

    private RouterKey() {}

    public static RouterKey build(String uri) {
        RouterKey key = new RouterKey();
        key.uri = Uri.parse(uri);
        return key;
    }

    public void setThread(@Extend.Thread int thread) {
        this.thread = thread;
    }

    @SafeVarargs
    public final RouterKey setInterceptor(Class<? extends IRouterInterceptor>... interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    public final RouterKey setInterceptorName(String... interceptorName) {
        this.interceptorName = interceptorName;
        return this;
    }

    public RouterKey setHold(boolean hold) {
        this.hold = hold;
        return this;
    }

    public RouterKey setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public RouterKey setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        return this;
    }
}
