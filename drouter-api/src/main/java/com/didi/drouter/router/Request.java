package com.didi.drouter.router;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;

import java.util.concurrent.atomic.AtomicInteger;

import static com.didi.drouter.api.Extend.REQUEST_BUILD_URI;

/**
 * Created by gaowei on 2018/8/31
 */
public class Request extends DataExtras<Request> {

    private static final AtomicInteger counter = new AtomicInteger(0);

    private final Uri uri;
    Context context;
    LifecycleOwner lifecycleOwner;
    String authority;
    @RouterType int routerType;
    int resendStrategy;
    long holdTimeout;
    String serialNumber;
    IRouterInterceptor.IInterceptor interceptor;

    private Request(@NonNull Uri uri) {
        this.uri = uri;
        this.serialNumber = String.valueOf(counter.getAndIncrement());
        putExtra(REQUEST_BUILD_URI, uri.toString());
    }

    public static Request build(String uri) {
        return new Request(uri == null ? Uri.EMPTY : Uri.parse(uri));
    }

    public void start() {
        start(null, null);
    }

    public void start(Context context) {
        start(context, null);
    }

    public void start(Context context, RouterCallback callback) {
        this.context = context == null ? DRouter.getContext() : context;
        RouterLoader.build(this, callback).start();
    }

    public Context getContext() {
        return context;
    }

    public @NonNull Uri getUri() {
        return uri;
    }

    public @RouterType int getRouterType() {
        return routerType;
    }

    public @NonNull String getNumber() {
        return serialNumber;
    }

    public Request setHoldTimeout(long millisecond) {
        this.holdTimeout = millisecond;
        return this;
    }

    public Request setRemoteAuthority(String authority) {
        this.authority = authority;
        return this;
    }

    public Request setRemoteDeadResend(@Extend.Resend int strategy) {
        this.resendStrategy = strategy;
        return this;
    }

    public Request setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        return this;
    }

    public @NonNull IRouterInterceptor.IInterceptor getInterceptor() {
        if (interceptor == null) return new InterceptorHandler.Default();
        return interceptor;
    }
}
