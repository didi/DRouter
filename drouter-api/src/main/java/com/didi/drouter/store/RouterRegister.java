package com.didi.drouter.store;


import androidx.annotation.RestrictTo;

import com.didi.drouter.router.IRouterHandler;

/**
 * Created by gaowei on 2019/4/29
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RouterRegister implements IRegister {

    private RouterKey uriKey;
    private IRouterHandler handler;

    private ServiceKey<?> serviceKey;
    private Object service;

    public RouterRegister(RouterKey uriKey, IRouterHandler handler) {
        this.uriKey = uriKey;
        this.handler = handler;
    }

    public RouterRegister(ServiceKey<?> key, Object service) {
        this.serviceKey = key;
        this.service = service;
    }

    @Override
    public void unregister() {
        RouterStore.unregister(uriKey, handler);
        RouterStore.unregister(serviceKey, service);
    }
}
