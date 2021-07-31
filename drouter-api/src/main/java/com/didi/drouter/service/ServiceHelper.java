package com.didi.drouter.service;


import androidx.annotation.RestrictTo;

import com.didi.drouter.store.IRouterProxy;

/**
 * Created by gaowei on 2020/10/29
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ServiceHelper {

    public static <T> IRouterProxy getServiceProxy(ServiceLoader<T> loader, Class<?> clz) {
        return loader.serviceAgent.getRouterProxy(clz);
    }
}
