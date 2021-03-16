package com.didi.drouter.store;

import android.content.Context;
import android.support.annotation.RestrictTo;

/**
 * Created by gaowei on 2020/10/25
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IRouterProxy {

    // once has default constructor for service
    Object newInstance(Context context);

    // has @Remote annotation only
    Object execute(Object instance, String methodName, Object[] args) throws RemoteMethodMatchException;

    class RemoteMethodMatchException extends Exception {}
}
