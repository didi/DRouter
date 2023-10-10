package com.didi.drouter.annotation

/**
 * Created by gaowei on 2020/10/28
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.BINARY
)
annotation class Remote
