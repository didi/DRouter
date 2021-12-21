package com.didi.drouter.annotation;

import android.support.annotation.Keep;

import com.didi.drouter.api.Extend;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by gaowei on 2018/8/30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Keep
public @interface Service {

    /**
     * Use parent interface, parent class or itself.
     * Besides that, you can also use {@link com.didi.drouter.service.AnyAbility} which represent all its parent
     * interfaces, classes and itself.
     */
    Class<?>[] function();

    /**
     * This alias array will be matched with function array one by one.
     */
    String[] alias() default {};

    /**
     * This feature array will be matched with function array one by one.
     */
    Class<?>[] feature() default {};

    /**
     * From large to small
     */
    int priority() default 0;

    /**
     * Used for cache your service.
     */
    @Extend.Cache int cache() default Extend.Cache.NO;
}
