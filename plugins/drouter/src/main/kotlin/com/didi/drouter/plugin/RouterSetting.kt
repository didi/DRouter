package com.didi.drouter.plugin

import com.didi.drouter.utils.Logger

/**
 * Created by gaowei on 2023/5/9
 */
open class RouterSetting {

    var debug = false
    var incremental = true
    var cache = true
    var useActivityRouterClass = false
    var supportNoAnnotationActivity = false
    var pluginName = ""

    /**
     * For compatible with proxy plugin
     */
    open class Parse(setting: RouterSetting) {

        companion object {
            @JvmField
            var debug = false
        }
        var incremental = true
        var cache = true
        var useActivityRouterClass = false
        var supportNoAnnotationActivity = false
        var pluginName = ""

        init {
            try {
                debug = setting.debug
            } catch(e: Exception) {
                wrongVersionLog(e)
            }
            try {
                incremental = setting.incremental
            } catch(e: Exception) {
                wrongVersionLog(e)
            }
            try {
                cache = setting.cache
            } catch(e: Exception) {
                wrongVersionLog(e)
            }
            try {
                useActivityRouterClass = setting.useActivityRouterClass
            } catch(e: Exception) {
                wrongVersionLog(e)
            }
            try {
                supportNoAnnotationActivity = setting.supportNoAnnotationActivity
            } catch(e: Exception) {
                wrongVersionLog(e)
            }
            try {
                pluginName = setting.pluginName
            } catch(e: Exception) {
                wrongVersionLog(e)
            }
        }

        private fun wrongVersionLog(e: Exception) {
            Logger.d("Please use plugin-proxy >= 1.0.2, err = " + e.message)
        }

        @Override
        override fun toString(): String {
            return String.format("Setting = debug:%s | incremental:%s | cache:%s | " +
                    "useActivityRouterClass:%s | supportNoAnnotationActivity:%s",
                debug, incremental, cache, useActivityRouterClass, supportNoAnnotationActivity)
        }
    }
}