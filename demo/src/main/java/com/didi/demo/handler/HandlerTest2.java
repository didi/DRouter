package com.didi.demo.handler;

import android.support.annotation.NonNull;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/11
 */
@Router(scheme = "didi", host = "router", path = "/handler/test2")
public class HandlerTest2 implements IRouterHandler {


    @Override
    public void handle(@NonNull final Request request, @NonNull final Result result) {

        RouterLogger.toast("主进程HandlerTest2执行成功");

    }
}
