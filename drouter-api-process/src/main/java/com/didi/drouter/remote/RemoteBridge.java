package com.didi.drouter.remote;

import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.annotation.Service;
import com.didi.drouter.api.Strategy;
import com.didi.drouter.service.IRemoteBridge;
import com.didi.drouter.utils.RouterLogger;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by gaowei on 2018/11/01
 *
 * One service build will create one RemoteBridge instance,
 * but with multiple RemoteCommand when executes.
 */
@Service(function = IRemoteBridge.class)
public class RemoteBridge implements IRemoteBridge {

    Strategy strategy;
    WeakReference<LifecycleOwner> lifecycle;
    private boolean reTry;

    @Override @SuppressWarnings("unchecked")
    public <T> T getService(@NonNull Strategy strategy, WeakReference<LifecycleOwner> lifecycle,
                            Class<T> serviceClass, String alias, Object feature, @Nullable Object... constructor) {
        this.strategy = strategy;
        this.lifecycle = lifecycle;
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] {serviceClass}, new Handler(serviceClass, alias, feature, constructor));
    }

    private class Handler implements InvocationHandler {

        private final Class<?> serviceClass;
        private final String alias;
        private final Object feature;
        private final Object[] constructorArgs;

        Handler(Class<?> serviceClass, String alias, Object feature, @Nullable Object... constructorArgs) {
            this.serviceClass = serviceClass;
            this.alias = alias;
            this.feature = feature;
            this.constructorArgs = constructorArgs;
        }

        @Override
        public Object invoke(Object proxy, Method method, @Nullable Object[] args) {
            final RemoteCommand command = new RemoteCommand();
            command.bridge = RemoteBridge.this;
            command.serviceClass = serviceClass;
            command.alias = alias;
            command.feature = feature;
            command.constructorArgs = constructorArgs;
            command.methodName = method.getName();
            command.methodArgs = args;
            RemoteResult result = execute(command);
            if (result != null && RemoteResult.SUCCESS.equals(result.state)) {
                return result.result;
            } else {
                Class<?> returnType = method.getReturnType();   // void、int...
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) {
                        return false;
                    } else if (returnType == char.class) {
                        return '0';
                    } else {
                        return 0;
                    }
                } else {
                    return null;
                }
            }
        }
    }

    @Nullable
    RemoteResult execute(RemoteCommand command) {
        RouterLogger.getCoreLogger().d("[Client] start " +
                "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        RouterLogger.getCoreLogger().d(
                "[Client] \"%s\" start, authority \"%s\"", command, strategy.authority);

        IHostService hostService = RemoteProvider.getHostService(strategy.authority);
        RemoteResult result = null;
        if (hostService != null) {
            try {
                RemoteResend.tryPrepareResend(this, command);
                if (strategy.callAsync) {
                    hostService.callAsync(command);
                    result = new RemoteResult(RemoteResult.SUCCESS);
                } else {
                    result = hostService.call(command);
                }
                if (result != null) {
                    if (RemoteResult.SUCCESS.equals(result.state)) {
                        RouterLogger.getCoreLogger().d(
                                "[Client] \"%s\" finish, and state success", command);
                    } else {
                        RouterLogger.getCoreLogger().e(
                                "[Client] \"%s\" finish, and state fail", command);
                    }
                } else {
                    RouterLogger.getCoreLogger().e(
                            "[Client] \"%s\" finish, remote inner error with early termination", command);
                }
            } catch (RemoteException e) {     // DeadObjectException
                RouterLogger.getCoreLogger().e("[Client] \"%s\" finish, RemoteException: %s", command, e);
                if (!reTry) {
                    reTry = true;
                    RemoteProvider.removeHostService(strategy.authority);
                    RouterLogger.getCoreLogger().w("[Client] retry execute: %s", command);
                    return execute(command);
                }
            } catch (RuntimeException e) {    // remote exception will send here in some cases
                RouterLogger.getCoreLogger().e("[Client] \"%s\" finish, RuntimeException: %s", command, e);
            }
        } else {
            RouterLogger.getCoreLogger().e("[Client] \"%s\" finish, server binder is null", command);
        }
        RouterLogger.getCoreLogger().d("[Client] finish " +
                "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        return result;
    }

    /**
     * This method can get remote binder after execute one remote command
     */
    public static IBinder getHostBinder(String authority) {
        IHostService service = RemoteProvider.getHostService(authority);
        return service != null ? service.asBinder() : null;
    }

}
