package com.didi.drouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by gaowei on 2018/8/30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Service {

    Class<?>[] function();

    String[] alias() default {};

    Class[] feature() default {};

    int priority() default 0;

    int cache() default 0;
}
