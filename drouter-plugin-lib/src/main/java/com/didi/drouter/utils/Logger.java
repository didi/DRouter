package com.didi.drouter.utils;

import com.didi.drouter.plugin.RouterSetting;

public class Logger {

    public static void p(Object msg) {
        System.out.println(msg);
    }

    public static void v(Object msg) {
        System.out.println("\033[36m" + msg + "\033[0m");
    }

    public static void d(Object msg) {
        if (RouterSetting.Parse.debug) {
            System.out.println("\033[37m" + msg + "\033[0m");
        }
    }

    public static void w(Object msg) {
        System.out.println("\033[32m" + msg + "\033[0m");
    }

    public static void e(Object msg) {
        System.out.println("\033[31m" + msg + "\033[0m");
    }
}
