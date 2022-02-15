package com.didi.drouter.router;

import android.os.Bundle;

import androidx.annotation.RestrictTo;

import com.didi.drouter.api.Strategy;

import java.util.Map;

/**
 * Created by gaowei on 2022/01/25
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IRequestProxy {

    String ROUTER_SIZE = "router_request_router_size";
    String IS_ACTIVITY_STARTED = "router_request_activity_started";

    void request(Request request, Strategy strategy, RemoteCallback callback);

    interface RemoteCallback {
        void data(Bundle bundle, Map<String, Object> map);
    }
}