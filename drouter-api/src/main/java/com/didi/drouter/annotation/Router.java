package com.didi.drouter.annotation;

import android.support.annotation.Keep;

import com.didi.drouter.api.Extend;
import com.didi.drouter.router.IRouterInterceptor;
import com.didi.drouter.router.Request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by gaowei on 2019/1/30
 * used for Activity, Fragment, View, IRouterHandler
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Keep
public @interface Router {

    /**
     * RegExp support.
     * uri can be combination of [scheme]://[host][path].
     */
    String uri() default "";

    /**
     * RegExp support.
     * If you want to match all strings, please use ".*", as setting to "" means support "" only.
     */
    String scheme() default "";

    /**
     * Same as scheme.
     */
    String host() default "";

    /**
     * Same as scheme.
     */
    String path() default "";

    /**
     * Assign interceptors.
     */
    Class<? extends IRouterInterceptor>[] interceptor() default {};

    String[] interceptorName() default {};

    /**
     * Used for IRouterHandler
     */
    @Extend.Thread int thread() default Extend.Thread.POSTING;

    /**
     * From large to small
     */
    int priority() default 0;

    /**
     * For Activity and IRouterHandler.
     * When you want to return data asynchronously, you can set true.
     * At the same time, you must execute
     * {@link com.didi.drouter.router.RouterHelper#release(Request)}}
     * in any place after hold task is done.
     *
     * This take effect only when start with RouterCallback
     */
    boolean hold() default false;
}
