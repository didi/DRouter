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
@Router(path = "/handler/test1", priority = 1)
public class HandlerTest1 implements IRouterHandler {

    @Override
    public void handle(@NonNull Request request, @NonNull Result result) {
        RouterLogger.toast("主进程HandlerTest1执行成功");
    }
}
