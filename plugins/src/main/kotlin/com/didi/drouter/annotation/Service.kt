package com.didi.drouter.annotation

import kotlin.reflect.KClass

/**
 * Created by gaowei on 2018/8/30
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Service(
    val function: Array<KClass<*>>,
    val alias: Array<String> = [],
    val feature: Array<KClass<*>> = [],
    val priority: Int = 0,
    val cache: Int = 0
)
