package com.didi.demo.interceptor;

import android.support.annotation.NonNull;

import com.didi.drouter.annotation.Interceptor;
import com.didi.drouter.api.Extend;
import com.didi.drouter.router.IRouterInterceptor;
import com.didi.drouter.router.Request;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/3
 */
@Interceptor(priority = 2, cache = Extend.Cache.SINGLETON)
public class InterceptorTest2 implements IRouterInterceptor {

    public InterceptorTest2() {
        RouterLogger.getAppLogger().d("InterceptorTest2 create");
    }

    @Override
    public void handle(@NonNull Request request) {
        request.getInterceptor().onContinue();
    }


}
