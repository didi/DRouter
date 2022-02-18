package com.didi.demo.remote;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SharedMemory;

import androidx.annotation.RequiresApi;

import com.didi.drouter.annotation.Assign;
import com.didi.drouter.annotation.Service;
import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.ResultObject;
import com.didi.drouter.module_base.remote.IRemoteFunction;
import com.didi.drouter.module_base.remote.RemoteFeature;
import com.didi.drouter.remote.IRemoteCallback;
import com.didi.drouter.utils.RouterLogger;

import java.nio.ByteBuffer;
import java.util.Collections;
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
    public ResultObject handle(ParamObject[] x, ParamObject y, Integer z, Context context, final IRemoteCallback.Type2<String, Integer> callback) {

//        RouterExecutor.main(new Runnable() {
//            @Override
//            public void run() {
//                RouterLogger.toast("主进程RemoteFunction执行成功");
//            }
//        });
//        RouterLogger.getAppLogger().d("RemoteFunction handle: TestBean[] x, String y=%s, int z=%s, %s", y, z, callback);
        final ResultObject result = new ResultObject();
        result.a = 100;
        result.i = "100";

        try {
            callback.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    RouterLogger.getAppLogger().d("Client death");
                }
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                HashMap<String, Object> map = new HashMap<>();
//                map.put("result", result);
//                if (callback.asBinder().isBinderAlive()) {
//                    callback.callback("aaa", 1);
//                }
//            }
//        }, 3000);

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

    @RequiresApi(api = Build.VERSION_CODES.O_MR1)
    @Override
    public void trans(final List<SharedMemory> memory) {

        for (SharedMemory memory1 : memory) {
            ByteBuffer buffer = null;
            try {
                buffer = memory1.mapReadWrite();
                RouterLogger.getCoreLogger().e("print %s ", buffer.capacity());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
//        RouterExecutor.main(new Runnable() {
//            @RequiresApi(api = Build.VERSION_CODES.O_MR1)
//            @Override
//            public void run() {
//
//                try {
//                    ByteBuffer buffer = memory.mapReadWrite();
//                    for (int i = 0 ; i < buffer.capacity() ; i++) {
//                        RouterLogger.getCoreLogger().e("print %s %s:", buffer.capacity(), buffer.get(i));
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                RouterExecutor.main(this, 3000);
//            }
//        });
    }

    private static Set<IRemoteCallback> callbacks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void register(IRemoteCallback.Type0 callback) {
        if (callbacks.contains(callback)) {
            RouterLogger.getAppLogger().e("RemoteRegister 重复注册");
        } else {
            callbacks.add(callback);
            RouterLogger.getAppLogger().d("RemoteRegister 注册成功");
        }
    }

    @Override
    public void unregister(IRemoteCallback.Type0 callback) {
        if (callbacks.contains(callback)) {
            callbacks.remove(callback);
            RouterLogger.getAppLogger().d("RemoteRegister 反注册成功");
        } else {
            RouterLogger.getAppLogger().e("RemoteRegister 反注册失败");
        }
    }

    @Override
    public void kill() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                int i = 1/0;
            }
        }, 2000);
    }

    public void call() {
    }

    public Integer cal(Integer a) {
        return 1;
    }



}
