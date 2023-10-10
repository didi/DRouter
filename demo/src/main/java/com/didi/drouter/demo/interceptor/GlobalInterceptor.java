package com.didi.drouter.demo.interceptor;

import androidx.annotation.NonNull;

import com.didi.drouter.annotation.Interceptor;
import com.didi.drouter.router.IRouterInterceptor;
import com.didi.drouter.router.Request;
import com.didi.drouter.utils.RouterLogger;


/**
 * Created by gaowei on 2018/9/7
 */
@Interceptor(priority = 1, global = true)
public class GlobalInterceptor implements IRouterInterceptor {

    public GlobalInterceptor() {
        RouterLogger.getAppLogger().d("GlobalInterceptor create");
    }

    @Override
    public void handle(@NonNull final Request request) {

        if (request.getUri().toString().equals("/activity/interceptor")) {
            request.getInterceptor().onInterrupt(404);   // 自定义错误码
            return;
        }

        // 可以重定向
        request.setRedirect(request.getUri().toString());
        request.getInterceptor().onContinue();
    }
}
