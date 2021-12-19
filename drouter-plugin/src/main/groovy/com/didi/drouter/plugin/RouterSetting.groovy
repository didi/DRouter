package com.didi.drouter.plugin

import com.didi.drouter.utils.Logger

/**
 * Created by gaowei on 2018/9/17
 */
class RouterSetting {

    boolean debug = false
    boolean incremental = true
    boolean cache = true
    boolean useActivityRouterClass = false
    boolean supportNoAnnotationActivity = false
    String pluginName

    /**
     * For compatible with proxy plugin
     */
    static class Parse {

        static boolean debug
        boolean incremental
        boolean cache
        boolean useActivityRouterClass
        boolean supportNoAnnotationActivity
        String pluginName

        Parse(RouterSetting setting) {
            try {
                debug = setting.debug
            } catch(Exception e) {
                log(e)
            }
            try {
                incremental = setting.incremental
            } catch(Exception e) {
                log(e)
            }
            try {
                cache = setting.cache
            } catch(Exception e) {
                log(e)
            }
            try {
                useActivityRouterClass = setting.useActivityRouterClass
            } catch(Exception e) {
                log(e)
            }
            try {
                supportNoAnnotationActivity = setting.supportNoAnnotationActivity
            } catch(Exception e) {
                log(e)
            }
            try {
                pluginName = setting.pluginName
            } catch(Exception e) {
                log(e)
            }
        }

        static void log(Exception e) {
            Logger.w("Please use plugin-proxy >= 1.0.2, err = " + e.message)
        }

        @Override
        String toString() {
            return String.format("Setting = debug:%s | incremental:%s | cache:%s | " +
                    "useActivityRouterClass:%s | supportNoAnnotationActivity:%s",
                    debug, incremental, cache, useActivityRouterClass, supportNoAnnotationActivity)
        }
    }
}