package com.didi.drouter.remote;

import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.service.ServiceHelper;
import com.didi.drouter.service.ServiceLoader;
import com.didi.drouter.store.IRouterProxy;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaowei on 2018/11/2
 */
class RemoteDispatcher {

    private static final AtomicInteger count = new AtomicInteger(0);
    private final RemoteResult remoteResult = new RemoteResult(RemoteResult.EXECUTING);

    @NonNull
    RemoteResult execute(final RemoteCommand command) {
        count.incrementAndGet();
        RouterLogger.getCoreLogger().d("[Service] command \"%s\" start, thread count %s", command, count.get());
        if (count.get() >= 16) {
            RouterLogger.getCoreLogger().e("[Service] binder thread pool is exploding", command, count.get());
        }
        if (command.uri != null) {
            if (count.get() >= 16) {
                RouterExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        startRequest(command);
                    }
                });
            } else {
                startRequest(command);
            }
        } else if (command.serviceClass != null) {
            startService(command);
        }
        count.decrementAndGet();
        return remoteResult;
    }

    private void startRequest(final RemoteCommand command) {
        Request request = DRouter.build(command.uri);
        if (command.extra != null) {
            request.extra = command.extra;
        }
        if (command.addition != null) {
            request.addition = command.addition;
        }
        request.start(DRouter.getContext(), new RouterCallback() {
            @Override
            public void onResult(@NonNull Result result) {
                if (command.binder != null) {
                    RouterLogger.getCoreLogger().d("[Service] command \"%s\" result start callback", command);
                    RemoteCommand resultCommand = new RemoteCommand(RemoteCommand.REQUEST_RESULT);
                    resultCommand.isActivityStarted = result.isActivityStarted();
                    resultCommand.routerSize = result.getRouterSize();
                    resultCommand.extra = result.getExtra();
                    resultCommand.addition = result.getAddition();
                    try {
                        IClientService.Stub.asInterface(command.binder).callback(resultCommand);
                    } catch (RemoteException e) {
                        RouterLogger.getCoreLogger().e(
                                "[Service] command \"%s\" callback Exception %s", command, e);
                    }
                }
            }
        });
        remoteResult.state = RemoteResult.SUCCESS;
    }

    private void startService(final RemoteCommand command) {
        ServiceLoader<?> loader = DRouter.build(command.serviceClass)
                .setAlias(command.alias)
                .setFeature(command.feature);
        Object instance = loader.getService(command.constructor);
        try {
            if (instance != null) {
                IRouterProxy serviceProxy = ServiceHelper.getServiceProxy(loader, instance.getClass());
                boolean useProxy = false;
                if (serviceProxy != null) {
                    try {
                        String methodName = command.methodName + "_$$_" +
                                (command.parameters == null ? 0 : command.parameters.length);
                        remoteResult.result = serviceProxy.execute(instance, methodName, command.parameters);
                        useProxy = true;
                    } catch (IRouterProxy.RemoteMethodMatchException ignore) {
                    }
                }
                if (!useProxy) {
                    remoteResult.result = ReflectUtil.invokeMethod(instance, command.methodName, command.parameters);
                }
                remoteResult.state = RemoteResult.SUCCESS;
                return;
            }
        } catch (Exception e) {
            RouterLogger.getCoreLogger().e("[Service] invoke Exception %s", e);
        }
        remoteResult.state = RemoteResult.FAIL;
    }
}
