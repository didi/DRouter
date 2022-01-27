package com.didi.drouter.store;

import android.content.Context;

import androidx.annotation.RestrictTo;

/**
 * Created by gaowei on 2020/10/25
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IRouterProxy {

    /**
     * This method contains Fragment/View/RouterHandler with @Router, and Interceptor with @Interceptor.
     * @return instance
     */
    Object newInstance(Context context);

    /**
     * This method contains only using @Remote annotation on method.
     * @return method result
     * @throws RemoteMethodMatchException when execute remote service method with no @Remote
     */
    Object callMethod(Object instance, String methodName, Object[] args) throws RemoteMethodMatchException;

    /**
     * Throw this when remote service method has no @Remote
     */
    class RemoteMethodMatchException extends Exception {}
}
