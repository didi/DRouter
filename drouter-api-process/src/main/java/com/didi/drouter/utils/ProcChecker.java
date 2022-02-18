package com.didi.drouter.utils;

/**
 * Created by gaowei on 2022/02/15
 */
public class ProcChecker {

    private static boolean isApplicationCreated;

    private static boolean hasCustomApplication() {
        return false;
    }

    public static void checkApplication() {
        if (hasCustomApplication() && !isApplicationCreated) {
            synchronized (ProcChecker.class) {
                if (!isApplicationCreated) {
                    try {
                        ProcChecker.class.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }

    public static void setApplicationCreated() {
        synchronized (ProcChecker.class) {
            isApplicationCreated = true;
            ProcChecker.class.notifyAll();
        }
    }
}
