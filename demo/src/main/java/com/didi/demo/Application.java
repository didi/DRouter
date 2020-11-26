package com.didi.demo;

import android.content.Context;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/1
 */
public class Application extends android.app.Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();


        RouterLogger.getAppLogger().d("App Application onCreate");
        // 可选
        DRouter.init(this);

    }
}
