package com.didi.drouter.remote;

import androidx.annotation.NonNull;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.service.ServiceHelper;
import com.didi.drouter.service.ServiceLoader;
import com.didi.drouter.store.IRouterProxy;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/11/2
 */
class RemoteDispatcher {

    @NonNull
    RemoteResult call(final RemoteCommand command) {
        RemoteResult result = new RemoteResult(RemoteResult.EXECUTING);
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
                return result;
            }
        } catch (Exception e) {
            RouterLogger.getCoreLogger().e("[Server] invoke Exception %s", e);
        }
        result.state = RemoteResult.FAIL;
        return result;
    }
}
