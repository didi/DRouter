package com.didi.drouter.demo.handler;

import androidx.annotation.NonNull;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;

/**
 * Created by gaowei on 2018/9/11
 */
@Router(path = "/handler/test1", priority = 1)
public class HandlerTest1 implements IRouterHandler {

    @Override
    public void handle(@NonNull Request request, @NonNull Result result) {
//        RouterLogger.toast("主进程HandlerTest1执行成功");

        result.putExtra("a", 1);
        result.putAddition("b", 2);
    }
}
