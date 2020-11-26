package com.didi.drouter.utils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;
import android.util.Log;

import com.didi.drouter.api.DRouter;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by gaowei on 2018/11/5
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SystemUtil {

    private static Application sApplication;
    private static String sProcessName;

    public static Application getApplication() {
        return sApplication;
    }

    public static void setApplication(Application application) {
        if (application != null) {
            SystemUtil.sApplication = application;
        }
    }

    public static String getProcessName() {

        if (!TextUtils.isEmpty(sProcessName)) {
            return sProcessName;
        }

        if (Build.VERSION.SDK_INT >= 28) {
            sProcessName = Application.getProcessName();
        } else {
            // Using the same technique as Application.getProcessName() for older devices
            // Using reflection since ActivityThread is an internal API
            try {
                @SuppressLint("PrivateApi")
                Class<?> activityThread = Class.forName("android.app.ActivityThread");

                // Before API 18, the method was incorrectly named "currentPackageName",
                // but it still returned the process name
                // See https://github.com/aosp-mirror/platform_frameworks_base/commit/b57a50bd16ce25db441da5c1b63d48721bb90687
                String methodName =
                        Build.VERSION.SDK_INT >= 18 ? "currentProcessName" : "currentPackageName";

                Method getProcessName = activityThread.getDeclaredMethod(methodName);
                sProcessName = (String) getProcessName.invoke(null);
            } catch (Exception e) {
                Log.e(RouterLogger.NAME, "getProcessName exception: " + e.getMessage());
            }
        }
        if (!TextUtils.isEmpty(sProcessName)) {
            return sProcessName;
        }

        try {
            Context context = DRouter.getContext();
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
                if (runningApps != null) {
                    for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
                        if (processInfo.pid == android.os.Process.myPid()) {
                            sProcessName = processInfo.processName;
                            return sProcessName;
                        }
                    }
                }
            }
            sProcessName = context.getPackageName();
        } catch (Exception e) {
            Log.e(RouterLogger.NAME, "getProcessName exception: " + e.getMessage());
        }

        return sProcessName;
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
