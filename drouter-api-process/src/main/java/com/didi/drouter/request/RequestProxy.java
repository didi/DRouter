package com.didi.drouter.request;

import android.os.Bundle;

import com.didi.drouter.annotation.Service;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Strategy;
import com.didi.drouter.remote.IRemoteCallback;
import com.didi.drouter.router.IRequestProxy;
import com.didi.drouter.router.Request;

import java.util.Map;

// client
@Service(function = IRequestProxy.class)
public class RequestProxy implements IRequestProxy {

    @Override
    public void request(Request request, Strategy strategy, final RemoteCallback callback) {
        DRouter.build(IRequestServer.class)
                .setRemote(strategy)
                .getService()
                .request(request.getUri().toString(), request.extra, request.addition,
                        new IRemoteCallback.Type2<Bundle, Map<String, Object>>() {
                            @Override
                            public void callback(Bundle p1, Map<String, Object> p2) {
                                callback.data(p1, p2);
                            }
                        });
    }
}