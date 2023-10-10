package com.didi.drouter.annotation

import kotlin.reflect.KClass

/**
 * Created by gaowei on 2019/1/30
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Router(
    val uri: String = "",
    val scheme: String = "",
    val host: String = "",
    val path: String = "",
    val interceptor: Array<KClass<*>> = [],
    val interceptorName: Array<String> = [],
    val thread: Int = 0,
    val priority: Int = 0,
    val hold: Boolean = false
)
