package com.didi.drouter.annotation

/**
 * Created by gaowei on 2018/9/26
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
annotation class Assign(val name: String = "")
