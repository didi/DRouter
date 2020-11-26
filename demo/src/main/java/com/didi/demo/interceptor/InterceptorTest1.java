package com.didi.demo.interceptor;

import android.support.annotation.NonNull;

import com.didi.drouter.router.IRouterInterceptor;
import com.didi.drouter.router.Request;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/10
 */
public class InterceptorTest1 implements IRouterInterceptor {


    public InterceptorTest1() {
        RouterLogger.getAppLogger().d("InterceptorTest1 create");
    }

    @Override
    public void handle(@NonNull Request request) {


        request.getInterceptor().onContinue();
    }

}
