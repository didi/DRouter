package com.didi.drouter.annotation;

import com.didi.drouter.api.Extend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by gaowei on 2018/9/3
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Interceptor {

    // from large to small
    int priority() default 0;

    boolean global() default false;

    @Extend.Cache int cache() default Extend.Cache.NO;
}
