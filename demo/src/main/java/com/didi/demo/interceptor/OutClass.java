package com.didi.demo.interceptor;

import android.support.annotation.NonNull;

import com.didi.drouter.router.IRouterInterceptor;
import com.didi.drouter.router.Request;

/**
 * Created by gaowei on 2018/11/15
 */
public class OutClass {


    public static class InnerInterceptor implements IRouterInterceptor {
        @Override
        public void handle(@NonNull Request request) {
            request.getInterceptor().onContinue();
        }

    }
}
