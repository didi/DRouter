package com.didi.drouter.remote;

import androidx.annotation.NonNull;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.service.ServiceHelper;
import com.didi.drouter.service.ServiceLoader;
import com.didi.drouter.store.IRouterProxy;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterLogger;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaowei on 2018/11/2
 */
class RemoteDispatcher {

    private static final AtomicInteger count = new AtomicInteger(0);
    private final RemoteResult result = new RemoteResult(RemoteResult.EXECUTING);

    @NonNull
    RemoteResult call(final RemoteCommand command) {
        count.incrementAndGet();
        if (count.get() >= 16) {
            RouterLogger.getCoreLogger().e(
                    "[Server] binder thread pool %s is exploding, \"%s\"", count.get(), command);
        }
        callService(command);
        count.decrementAndGet();
        return result;
    }

    private void callService(final RemoteCommand command) {
        ServiceLoader<?> loader = DRouter.build(command.serviceClass)
                .setAlias(command.alias)
                .setFeature(command.feature);
        Object instance = loader.getService(command.constructorArgs);
        try {
            if (instance != null) {
                IRouterProxy serviceProxy = ServiceHelper.getServiceProxy(loader, instance.getClass());
                boolean useProxy = false;
                if (serviceProxy != null) {
                    try {
                        String methodName = command.methodName + "_$$_" +
                                (command.methodArgs == null ? 0 : command.methodArgs.length);
                        result.result = serviceProxy.callMethod(instance, methodName, command.methodArgs);
                        useProxy = true;
                    } catch (IRouterProxy.RemoteMethodMatchException ignore) {
                    }
                }
                if (!useProxy) {
                    result.result = ReflectUtil.invokeMethod(instance, command.methodName, command.methodArgs);
                }
                RouterLogger.getCoreLogger().d("[Server] \"%s\" execute success", command);
                result.state = RemoteResult.SUCCESS;
                return;
            }
        } catch (Exception e) {
            RouterLogger.getCoreLogger().e("[Server] invoke Exception %s", e);
        }
        result.state = RemoteResult.FAIL;
    }
}
