package com.didi.drouter.store;

import android.arch.lifecycle.LifecycleOwner;
import android.net.Uri;

import com.didi.drouter.api.Extend;
import com.didi.drouter.router.IRouterInterceptor;
import com.didi.drouter.utils.TextUtils;

/**
 * Created by gaowei on 2019/1/31
 */
public class RouterKey {

    Uri uri;
    Class<? extends IRouterInterceptor>[] interceptor;
    int thread;
    boolean hold;
    LifecycleOwner lifecycleOwner;

    private RouterKey() {}

    public static RouterKey build(String uri) {
        RouterKey key = new RouterKey();
        key.uri = TextUtils.getUriKey(uri);
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

    public RouterKey setHold(boolean hold) {
        this.hold = hold;
        return this;
    }

    public RouterKey setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        return this;
    }
}
