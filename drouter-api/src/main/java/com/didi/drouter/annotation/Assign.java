package com.didi.drouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by gaowei on 2018/9/26
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Assign {

    String name() default "";
}
