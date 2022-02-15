package com.didi.drouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by gaowei on 2020/10/28
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Remote {
}
