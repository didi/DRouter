package com.didi.demo.handler;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.Extend;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterHelper;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/11
 */
@Router(path = "/handler/test3", hold = true, thread = Extend.Thread.WORKER)
public class HandlerTest3 implements IRouterHandler {


    @Override
    public void handle(@NonNull final Request request, @NonNull final Result result) {

        RouterLogger.toast("子线程HandlerTest3: " + Thread.currentThread().getName());

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                RouterHelper.release(request);
            }
        }, 2000);


    }
}
