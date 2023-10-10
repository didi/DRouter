package com.didi.drouter.annotation

/**
 * Created by gaowei on 2018/9/3
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Interceptor(
    val name: String = "",
    val priority: Int = 0,
    val global: Boolean = false,
    val cache: Int = 0
)
