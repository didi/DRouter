package com.didi.drouter.utils

import com.didi.drouter.plugin.RouterSetting.Parse

object Logger {
    fun p(msg: Any?) {
        println(msg)
    }

    fun v(msg: Any) {
        println("\u001b[36m$msg\u001b[0m")
    }

    fun d(msg: Any) {
        if (Parse.debug) {
            println("\u001b[37m$msg\u001b[0m")
        }
    }

    fun w(msg: Any) {
        println("\u001b[32m$msg\u001b[0m")
    }

    fun e(msg: Any) {
        println("\u001b[31m$msg\u001b[0m")
    }
}
