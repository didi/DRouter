package com.didi.demo.service;

import androidx.annotation.NonNull;

import com.didi.drouter.annotation.Service;
import com.didi.drouter.router.IRouterResult;
import com.didi.drouter.router.Request;
import com.didi.drouter.utils.RouterLogger;

/**
 * 返回路由结果
 */
@Service(function = IRouterResult.class)
public class RouterResult implements IRouterResult {

    @Override
    public void onResult(@NonNull Request request, int state) {
        RouterLogger.getAppLogger().d("result => uri: %s, type: %s, state: %s",
                request.getUri().toString(), request.getRouterType(), state);
    }
}
