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
class CmdDispatcher {

    @NonNull
    StreamResult call(final StreamCmd command) {
        StreamResult result = new StreamResult(StreamResult.EXECUTING);
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
                result.state = StreamResult.SUCCESS;
                return result;
            }
        } catch (Exception e) {
            RouterLogger.getCoreLogger().e("[Server] invoke Exception %s", e);
        }
        result.state = StreamResult.FAIL;
        return result;
    }
}
