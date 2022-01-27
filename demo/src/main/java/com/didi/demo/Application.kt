package com.didi.demo

import android.app.Application
import android.content.Context
import com.didi.drouter.api.DRouter
import com.didi.drouter.utils.RouterLogger

/**
 * Created by gaowei on 2018/9/1
 */
class Application : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        RouterLogger.getAppLogger().d("App Application attachBaseContext: $this")
    }

    override fun onCreate() {
        super.onCreate()
        RouterLogger.getAppLogger().d("App Application onCreate: $this")
        // 可选
        DRouter.init(this)
    }
}