package com.didi.drouter.router;

import static com.didi.drouter.api.Extend.REQUEST_BUILD_URI;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
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

    private Uri uri;
    Context context;
    Lifecycle lifecycle;
    Strategy strategy;
    @RouterType int routerType;
    long holdTimeout;
    String serialNumber;
    IRouterInterceptor.IInterceptor interceptor;
    boolean canRedirect = true;
    ActivityResultLauncher<Intent> launcher;

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

    public String getNumber() {
        return serialNumber;
    }

    public ActivityResultLauncher<Intent> getActivityResultLauncher() {
        return launcher;
    }

    public Request setActivityResultLauncher(ActivityResultLauncher<Intent> launcher) {
        this.launcher = launcher;
        return this;
    }

    /**
     * The router can only redirect in global interceptor or before.
     * Because target will be identified when related interceptor is reached.
     * The order of execution of interceptors is all global -> all related interceptor.
     * @param uri new uri
     */
    public Request setRedirect(String uri) {
        if (canRedirect) {
            this.uri = uri == null ? Uri.EMPTY : Uri.parse(uri);
        }
        return this;
    }

    public Request setHoldTimeout(long millisecond) {
        this.holdTimeout = millisecond;
        return this;
    }

    /**
     * @param strategy for IPC config
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
