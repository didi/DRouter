package com.didi.demo.remote;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.RouterLifecycle;
import com.didi.drouter.api.Strategy;
import com.didi.drouter.demo.R;
import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.remote.IRemoteFunction;
import com.didi.drouter.module_base.remote.RemoteFeature;
import com.didi.drouter.remote.IRemoteCallback;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Router(path = "/activity/remote")
public class RemoteActivity extends AppCompatActivity {

    private IRemoteFunction remoteFunction;
    private IRemoteFunction resentRemoteFunction;
    private final RouterLifecycle lifecycle = new RouterLifecycle();
    List<Object> xx = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
    }

    public void onClick(View view) {

        if (view.getId() == R.id.request) {

            RouterExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        DRouter.build("/handler/test1")
                                .putExtra("1", 1)
                                .putExtra("2", new Bundle())
                                .putAddition("3", new ParamObject())
                                .setLifecycle(RemoteActivity.this.getLifecycle())
                                .setRemote(new Strategy("com.didi.drouter.remote.demo.host"))
                                .start(DRouter.getContext(), new RouterCallback() {
                                    @Override
                                    public void onResult(@NonNull Result result) {
//                                        RouterLogger.toast("子进程收到主进程的回调");
                                        RouterLogger.getAppLogger().d("callback 参数 %s %s",
                                                result.getInt("a"), result.getAddition("b"));
                                    }
                                });
                        try {
                            Thread.sleep(1150);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        if (view.getId() == R.id.service) {
            bindRemote();
            RouterExecutor.submit(new Runnable() {
                @Override
                public void run() {
//                    while (true) {
                        xx.add(new Object());
                        remoteFunction.handle(new ParamObject[]{}, new ParamObject(), 2, RemoteActivity.this,
                                new IRemoteCallback.Type2<String, Integer>() {
                                    @Override
                                    public void callback(String s, Integer i) {
                                        RouterLogger.getAppLogger().d("callback 参数 %s %s", s, i);
//                                        RouterLogger.toast("子进程收到主进程的回调");
                                    }

                                    @Override
                                    public int thread() {
                                        return super.thread();
                                    }

                                    @Override
                                    public Lifecycle lifecycle() {
                                        return RemoteActivity.this.getLifecycle();
                                    }

                                    @Override
                                    protected void finalize() throws Throwable {
                                        super.finalize();
                                        RouterLogger.getAppLogger().e("client callback gc");
                                    }

                                    @Override
                                    protected void onServerDead() {
                                        super.onServerDead();
                                        RouterLogger.getAppLogger().e("onServerDead");
                                    }
                                });

                        try {
                            Thread.sleep(1100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
//                    }
                }
            });

        }

        if (view.getId() == R.id.resend_callback) {
            bindResendRemote();
            // 打开重试策略
            lifecycle.create();
            resentRemoteFunction.register(callback);
        }

        if (view.getId() == R.id.cancel_resend_callback) {
            bindResendRemote();
            // 先停止生命周期，来关闭重试策略
            lifecycle.destroy();
            resentRemoteFunction.unregister(callback);
        }

        if (view.getId() == R.id.kill_main) {
            bindRemote();
            remoteFunction.kill();
        }

        if (view.getId() == R.id.awake_main) {
            bindRemote();
            remoteFunction.call();
        }

        if (view.getId() == R.id.kill_self) {
            int i = 1 / 0;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lifecycle.destroy();
    }

    private final IRemoteCallback.Type0 callback = new IRemoteCallback.Type0() {
        @Override
        public void callback() {

        }
    };

    private void bindRemote() {
        if (remoteFunction == null) {
            final RemoteFeature feature = new RemoteFeature();
            feature.a = 1;
            feature.b = "1";

            final Map<String, ParamObject> map = new ConcurrentHashMap<>();
            map.put("param", new ParamObject());
            final List<ParamObject> list = new LinkedList<>();
            list.add(new ParamObject());
            final Set<ParamObject> set = new HashSet<>();
            set.add(new ParamObject());

            remoteFunction = DRouter.build(IRemoteFunction.class)
                    .setRemote(new Strategy("com.didi.drouter.remote.demo.host"))
                    .setAlias("remote")
                    .setFeature(feature)
                    .getService(new ParamObject[]{new ParamObject()}, map, list, set, 1);
        }
    }

    private void bindResendRemote() {
        if (resentRemoteFunction == null) {
            final RemoteFeature feature = new RemoteFeature();
            feature.a = 1;
            feature.b = "1";

            final Map<String, ParamObject> map = new ConcurrentHashMap<>();
            map.put("param", new ParamObject());
            final List<ParamObject> list = new LinkedList<>();
            list.add(new ParamObject());
            final Set<ParamObject> set = new HashSet<>();
            set.add(new ParamObject());

            resentRemoteFunction = DRouter.build(IRemoteFunction.class)
                    .setRemote(new Strategy("com.didi.drouter.remote.demo.host")
                            // 打开重试功能
                            .setResend(Strategy.Resend.WAIT_ALIVE))
                    .setAlias("remote")
                    .setFeature(feature)
                    // 如果设置了生命周期，则由生命周期来控制重试策略
                    // 当生命周期是create，重试功能打开
                    .setLifecycle(lifecycle.getLifecycle())
                    .getService(new ParamObject[]{new ParamObject()}, map, list, set, 1);
        }
    }

}
