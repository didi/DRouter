package com.didi.drouter.utils;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.RestrictTo;

import com.didi.drouter.api.DRouter;

/**
 * Created by gaowei on 2018/11/5
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SystemUtil {

    private static Application sApplication;
    static boolean isDebug;

    public static Application getApplication() {
        return sApplication;
    }

    public static void setApplication(Application application) {
        if (application != null) {
            initDebug(application);
            SystemUtil.sApplication = application;
        }
    }

    private static void initDebug(Application application) {
        if (sApplication != null) return;
        try {
            ApplicationInfo info = application.getApplicationInfo();
            isDebug = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception ignore) {
        }
        Log.d(RouterLogger.CORE_TAG, "drouter is debug: " + isDebug);
    }

    public static String getAppName() {
        try {
            int labelInfo = DRouter.getContext().getApplicationInfo().labelRes;
            return DRouter.getContext().getString(labelInfo);
        } catch (Exception e) {
            return null;
        }
    }

    public static synchronized String getPackageName() {
        try {
            return DRouter.getContext().getPackageName();
        } catch (Exception e) {
            return null;
        }
    }
}
