package com.didi.drouter.remote;

import android.app.Application;

import com.didi.drouter.api.DRouter;

/**
 * Created by gaowei on 2018/11/1
 */
public class RemoteApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();

        DRouter.init(this);
    }
}
