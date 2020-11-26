package com.didi.drouter.remote;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;
import com.didi.drouter.api.RouterLifecycle;
import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.remote.IRemoteFunction;
import com.didi.drouter.module_base.remote.RemoteFeature;
import com.didi.drouter.utils.RouterLogger;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MainActivity extends AppCompatActivity {
    private IRemoteFunction remoteFunction;
    private IRemoteFunction resentRemoteFunction;
    private final RouterLifecycle lifecycle = new RouterLifecycle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
    }

    public void onClick(View view) {

        if (view.getId() == R.id.request) {

            DRouter.build("/handler/test1")
                    .putExtra("1", 1)
                    .putExtra("2", new Bundle())
                    .putAddition("3", new ParamObject())
                    .setRemoteAuthority("com.didi.drouter.remote.demo.host")
                    .start(DRouter.getContext());
        }

        if (view.getId() == R.id.service) {
            bindRemote();
            remoteFunction.handle(new ParamObject[]{}, new ParamObject(), 2, this,
                    new IRemoteCallback() {
                        @Override
                        public void callback(Object... data) throws RemoteException {
                            RouterLogger.toast("子进程收到主进程的回调");
                        }
                    });
        }

        if (view.getId() == R.id.resend_callback) {
            bindResendRemote();
            lifecycle.create();
            resentRemoteFunction.register(callback);
        }

        if (view.getId() == R.id.cancel_resend_callback) {
            bindResendRemote();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lifecycle.destroy();
    }

    private final IRemoteCallback callback = new IRemoteCallback() {
        @Override
        public void callback(Object... data) throws RemoteException {

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
                    .setRemoteAuthority("com.didi.drouter.remote.demo.host")
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
                    .setRemoteAuthority("com.didi.drouter.remote.demo.host")
                    .setAlias("remote")
                    .setFeature(feature)
                    .setRemoteDeadResend(Extend.Resend.WAIT_ALIVE)
                    .setLifecycleOwner(lifecycle)
                    .getService(new ParamObject[]{new ParamObject()}, map, list, set, 1);
        }
    }


}
