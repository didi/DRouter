package com.didi.drouter.utils;

public class Logger {

    public static boolean debug;

    public static void p(Object msg) {
        System.out.println(msg);
    }

    public static void v(Object msg) {
        System.out.println("\033[36m" + msg + "\033[0m");
    }

    public static void d(Object msg) {
        if (debug) {
            System.out.println("\033[37m" + msg + "\033[0m");
        }
    }

    public static void w(Object msg) {
        System.out.println("\033[33;1m" + msg + "\033[0m");
    }

    public static void e(Object msg) {
        System.out.println("\033[31;1m" + msg + "\033[0m");
    }
}
