package com.didi.drouter.router;

import static com.didi.drouter.api.Extend.REQUEST_BUILD_URI;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Strategy;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaowei on 2018/8/31
 */
public class Request extends DataExtras<Request> {

    private static final AtomicInteger counter = new AtomicInteger(0);

    private final Uri uri;
    Context context;
    Lifecycle lifecycle;
    Strategy strategy;
    @RouterType int routerType;
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

    /**
     * @param context If you want to return ActivityResult,
     *                please use Activity for context and ActivityCallback for RouterCallback.
     * @param callback Result to return.
     */
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

    /**
     * @param strategy for remote process.
     */
    public Request setRemote(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public Request setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
        return this;
    }

    public @NonNull IRouterInterceptor.IInterceptor getInterceptor() {
        return interceptor == null ? new InterceptorHandler.Default() : interceptor;
    }
}
