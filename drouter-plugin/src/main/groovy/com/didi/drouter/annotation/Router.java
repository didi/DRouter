package com.didi.drouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by gaowei on 2019/1/30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Router {

    String scheme() default "";

    String host() default "";

    String path();

    Class[] interceptor() default {};

    int thread() default 0;

    int priority() default 0;

    boolean hold() default false;
}
