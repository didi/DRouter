package com.didi.drouter.router;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2018/8/31
 */
public interface IRouterInterceptor {

    void handle(@NonNull Request request);

    interface IInterceptor {

        void onContinue();

        /**
         * Return default statusCode {@link Result#INTERCEPT}
         */
        void onInterrupt();

        /**
         * @param statusCode Return custom statusCode
         */
        void onInterrupt(int statusCode);
    }
}
