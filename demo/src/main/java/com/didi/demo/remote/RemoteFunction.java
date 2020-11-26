package com.didi.demo.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import com.didi.drouter.annotation.Assign;
import com.didi.drouter.annotation.Remote;
import com.didi.drouter.annotation.Service;
import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.ResultObject;
import com.didi.drouter.module_base.remote.IRemoteFunction;
import com.didi.drouter.module_base.remote.RemoteFeature;
import com.didi.drouter.remote.IRemoteCallback;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by gaowei on 2018/11/2
 */
@Service(function = IRemoteFunction.class, alias = "remote", feature = RemoteFeature.class)
public class RemoteFunction implements IRemoteFunction {

    @Assign
    public static final int a = 1;

    @Assign
    public static final String b = "1";

    ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    public RemoteFunction() {
        RouterLogger.getAppLogger().d("RemoteFunction constructor argument");
    }

    public RemoteFunction(Integer i) {
        RouterLogger.getAppLogger().d("RemoteFunction constructor Integer argument");
    }

    public RemoteFunction(ParamObject[] x, Map<String, ParamObject> y, List<ParamObject> z, Set<ParamObject> a, int b) {
        RouterLogger.getAppLogger().d("RemoteFunction constructor argument: TestBean[] x, String y=%s, int z=%s", y, z);
    }

    @Override
    @Remote
    public ResultObject handle(ParamObject[] x, ParamObject y, Integer z, Context context, final IRemoteCallback callback) {

        RouterExecutor.main(new Runnable() {
            @Override
            public void run() {
                RouterLogger.toast("主进程RemoteFunction执行成功");
            }
        });
        RouterLogger.getAppLogger().d("RemoteFunction handle: TestBean[] x, String y=%s, int z=%s, %s", y, z, callback);
        final ResultObject result = new ResultObject();
        result.a = 100;
        result.i = "100";

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                HashMap<String, Object> map = new HashMap<>();
                map.put("result", result);
                try {
                    callback.callback(map);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }, 3000);

//        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                HashMap<String, Object> map = new HashMap<>();
//                map.put("result", result);
//                try {
//                    callback.callback(map);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, 0, 3, TimeUnit.SECONDS);
        return result;
    }

    private static Set<IRemoteCallback> callbacks =
            Collections.newSetFromMap(new ConcurrentHashMap<IRemoteCallback, Boolean>());

    @Override
    @Remote
    public void register(IRemoteCallback callback) {
        if (callbacks.contains(callback)) {
            RouterLogger.getAppLogger().e("RemoteRegister 重复注册");
        } else {
            callbacks.add(callback);
            RouterLogger.getAppLogger().d("RemoteRegister 注册成功");
        }
    }

    @Override
    @Remote
    public void unregister(IRemoteCallback callback) {
        if (callbacks.contains(callback)) {
            callbacks.remove(callback);
            RouterLogger.getAppLogger().d("RemoteRegister 反注册成功");
        } else {
            RouterLogger.getAppLogger().e("RemoteRegister 反注册失败");
        }
    }

    @Override
    @Remote
    public void kill() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                System.exit(0);
            }
        }, 2000);
    }

    @Override
    @Remote
    public void call() {

    }


}
