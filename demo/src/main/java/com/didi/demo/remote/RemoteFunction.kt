//package com.didi.demo.remote
//
//import android.content.Context
//import android.os.Handler
//import android.os.Looper
//import com.didi.drouter.annotation.Assign
//import com.didi.drouter.annotation.Remote
//import com.didi.drouter.annotation.Service
//import com.didi.drouter.module_base.ParamObject
//import com.didi.drouter.module_base.ResultObject
//import com.didi.drouter.module_base.remote.IRemoteFunction
//import com.didi.drouter.module_base.remote.RemoteFeature
//import com.didi.drouter.remote.IRemoteCallback
//import com.didi.drouter.utils.RouterExecutor
//import com.didi.drouter.utils.RouterLogger
//import java.util.*
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.Executors
//
///**
// * Created by gaowei on 2018/11/2
// */
//@Service(function = [IRemoteFunction::class], alias = ["remote"], feature = [RemoteFeature::class])
//class RemoteFunction : IRemoteFunction {
//    var scheduledExecutorService = Executors.newScheduledThreadPool(5)
//
//    constructor() {
//        RouterLogger.getAppLogger().d("RemoteFunction constructor argument")
//    }
//
//    constructor(i: Int?) {
//        RouterLogger.getAppLogger().d("RemoteFunction constructor Integer argument")
//    }
//
//    constructor(x: Array<ParamObject?>?, y: Map<String?, ParamObject?>?, z: List<ParamObject?>?, a: Set<ParamObject?>?, b: Int) {
//        RouterLogger.getAppLogger().d("RemoteFunction constructor argument: TestBean[] x, String y=%s, int z=%s", y, z)
//    }
//
//    @Remote
//    override fun handle(x: Array<ParamObject?>?, y: ParamObject?, z: Int?, context: Context?, callback: IRemoteCallback.Type2<String, Int>?): ResultObject? {
//        RouterExecutor.main { RouterLogger.toast("主进程RemoteFunction执行成功") }
//        RouterLogger.getAppLogger().d("RemoteFunction handle: TestBean[] x, String y=%s, int z=%s, %s", y, z, callback)
//        val result = ResultObject()
//        result.a = 100
//        result.i = "100"
//        callback!!.callback("str  ing aa", 3)
////        Handler(Looper.getMainLooper()).postDelayed({
////            val map = HashMap<String, Any>()
////            map["result"] = result
////            try {
////                callback!!.callback(map)
////            } catch (e: RemoteException) {
////                e.printStackTrace()
////            }
////        }, 3000)
//
////        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
////            @Override
////            public void run() {
////                HashMap<String, Object> map = new HashMap<>();
////                map.put("result", result);
////                try {
////                    callback.callback(map);
////                } catch (RemoteException e) {
////                    e.printStackTrace();
////                }
////            }
////        }, 0, 3, TimeUnit.SECONDS);
//        return result
//    }
//
//    @Remote
//    override fun register(callback: IRemoteCallback?) {
//        if (callbacks.contains(callback)) {
//            RouterLogger.getAppLogger().e("Remo teR egister 重复注册")
//        } else {
//            callbacks.add(callback)
//            RouterLogger.getAppLogger().d("RemoteRegister 注册成功")
//        }
//    }
//
//    @Remote
//    override fun unregister(callback: IRemoteCallback?) {
//        if (callbacks.contains(callback)) {
//            callbacks.remove(callback)
//            RouterLogger.getAppLogger().d("RemoteRegister 反注册成功")
//        } else {
//            RouterLogger.getAppLogger().e("RemoteRegister 反注册失败")
//        }
//    }
//
//    @Remote
//    override fun kill() {
//        Handler(Looper.getMainLooper()).postDelayed({ val i = 1 / 0 }, 2000)
//    }
//
//    @Remote
//    override suspend fun call() {
//    }
//
//    @Remote
//    fun cal(a: Int?): Int? {
//        return 1
//    }
//
//    public companion object {
//        @Assign
//        @JvmField
//        public val a = 1
//
//        @Assign
//        @JvmField
//        val b = "1"
//
//        private val callbacks = Collections.newSetFromMap(ConcurrentHashMap<IRemoteCallback, Boolean>())
//    }
//}