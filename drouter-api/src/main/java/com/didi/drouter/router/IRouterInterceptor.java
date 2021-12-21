package com.didi.drouter.router;

import android.support.annotation.NonNull;

/**
 * Created by gaowei on 2018/8/31
 */
public interface IRouterInterceptor {

    void handle(@NonNull Request request);

    interface IInterceptor {

        void onContinue();

        void onInterrupt();
    }
}
