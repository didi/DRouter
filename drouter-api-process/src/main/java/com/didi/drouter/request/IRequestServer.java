package com.didi.drouter.request;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.didi.drouter.annotation.Remote;
import com.didi.drouter.annotation.Service;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.remote.IRemoteCallback;
import com.didi.drouter.router.IRequestProxy;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.utils.RouterLogger;

import java.util.Map;

/**
 * Created by gaowei on 2022/02/15
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IRequestServer {

    void request(String uri, Bundle extra, Map<String, Object> addition,
                 IRemoteCallback.Type2<Bundle, Map<String, Object>> callback);

    // server
    @Service(function = IRequestServer.class)
    class RequestServer implements IRequestServer {

        @Override @Remote
        public void request(String uri, Bundle extra, Map<String, Object> addition,
                            final IRemoteCallback.Type2<Bundle, Map<String, Object>> callback) {
            final Request request = DRouter.build(uri);
            if (extra != null) {
                request.extra = extra;
            }
            if (addition != null) {
                request.addition = addition;
            }
            request.start(DRouter.getContext(), callback != null ? new RouterCallback() {
                @Override
                public void onResult(@NonNull Result result) {
                    RouterLogger.getCoreLogger().d("[Server] \"%s\" result start callback", request);
                    result.extra.putInt(IRequestProxy.ROUTER_SIZE, result.getRouterSize());
                    result.extra.putBoolean(IRequestProxy.IS_ACTIVITY_STARTED, result.isActivityStarted());
                    callback.callback(result.extra, result.addition);
                }
            } : null);
        }
    }
}
