package com.didi.drouter.remote;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.router.RouterHelper;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.didi.drouter.remote.RemoteProvider.BROADCAST_ACTION;
import static com.didi.drouter.remote.RemoteProvider.FIELD_REMOTE_LAUNCH_ACTION;

/**
 * Created by gaowei on 2018/11/1
 *
 * One service or request build will create one RemoteBridge instance,
 * but with multiple RemoteCommand when executes.
 *
 * Every resend RemoteCommand(execute) bind broadcast and one lifecycle observer if exists.
 */
public class RemoteBridge {

    private String authority;
    private int resendStrategy;
    private boolean reTry;
    private WeakReference<LifecycleOwner> lifecycle;

    // key is process, used for broadcast to resend
    private static final Map<String, Set<RemoteCommand>> sRetainCommandMap = new ConcurrentHashMap<>();
    // key is authority, value is process, one process register broadcast once
    private static final Map<String, String> sProcessMap = new ConcurrentHashMap<>();

    private RemoteBridge() {}

    public static @NonNull RemoteBridge load(String authority, int resend, WeakReference<LifecycleOwner> lifecycle) {
        RemoteBridge remoteBridge = new RemoteBridge();
        remoteBridge.authority = authority;
        remoteBridge.resendStrategy = resend;
        remoteBridge.lifecycle = lifecycle;
        return remoteBridge;
    }

    public void start(final Request request, final Result result, RouterCallback callback) {
        final RemoteCommand command = new RemoteCommand(RemoteCommand.REQUEST);
        command.bridge = RemoteBridge.this;
        command.resendStrategy = resendStrategy;
        command.lifecycle = lifecycle;
        command.uri = request.getUri().toString();
        command.extra = request.getExtra();
        command.addition = request.getAddition();
        if (callback != null) {
            command.binder = new IClientService.Stub() {
                @Override
                public RemoteResult callback(RemoteCommand resultCommand) {
                    RouterLogger.getCoreLogger().w("[Client] \"%s\" callback success", command);
//                    result.setActivityStarted(resultCommand.isActivityStarted);
                    result.extra = resultCommand.extra;
                    result.addition = resultCommand.addition;
                    // callback once, so release it
                    RouterHelper.release(request);
                    return null;
                }
            };
        } else {
            // no callback, so release immediately
            RouterHelper.release(request);
        }
        execute(command);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass, String alias, Object feature, @Nullable Object... constructor) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] {serviceClass}, new RemoteHandler(serviceClass, alias, feature, constructor));
    }

    private class RemoteHandler implements InvocationHandler {

        private final Class<?> serviceClass;
        private final String alias;
        private final Object feature;
        private final Object[] constructor;

        RemoteHandler(Class<?> serviceClass, String alias, Object feature, @Nullable Object... constructor) {
            this.serviceClass = serviceClass;
            this.alias = alias;
            this.feature = feature;
            this.constructor = constructor;
        }

        @Override
        public Object invoke(Object proxy, Method method, @Nullable Object[] parameters) {
            final RemoteCommand command = new RemoteCommand(RemoteCommand.SERVICE);
            command.bridge = RemoteBridge.this;
            command.lifecycle = lifecycle;
            command.resendStrategy = resendStrategy;
            command.serviceClass = serviceClass;
            command.alias = alias;
            command.feature = feature;
            command.constructor = constructor;
            command.methodName = method.getName();
            command.parameters = parameters;
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
    private RemoteResult execute(RemoteCommand command) {
        RouterLogger.getCoreLogger().d("[Client] start " +
                "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        RouterLogger.getCoreLogger().d(
                "[Client] \"%s\" start, authority \"%s\"", command, authority);

        IHostService hostService = RemoteProvider.getHostService(authority);
        RemoteResult result = null;
        if (hostService != null) {
            try {
                retainCommandIfNeeded(hostService, command);
                result = hostService.execute(command);
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
                    RemoteProvider.removeHostService(authority);
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

    private void retainCommandIfNeeded(IHostService hostService, final RemoteCommand command) throws RemoteException {
        if (command.resendStrategy == Extend.Resend.WAIT_ALIVE) {
            String process = sProcessMap.get(authority);
            if (process == null) {
                synchronized (RemoteBridge.class) {
                    process = sProcessMap.get(authority);
                    if (process == null) {
                        process = hostService.getProcess();
                        if (!TextUtils.isEmpty(process)) {
                            sProcessMap.put(authority, process);
                            registerBroadcast(process);
                        }
                    }
                }
            }
            if (TextUtils.isEmpty(process)) {
                RouterLogger.getCoreLogger().e("[Client] retain command fail, for process name is null");
                return;
            }
            // If lifecycle exists, resend command can be removed when destroyed
            // check lifecycle state
            LifecycleOwner owner;
            final Lifecycle lifecycle = command.lifecycle != null ?
                    ((owner = command.lifecycle.get()) != null ? owner.getLifecycle() : null) : null;
            if (lifecycle != null && lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
                RouterLogger.getCoreLogger().e("[Client] retain command fail, for lifecycle is assigned but destroyed");
                return;
            }
            Set<RemoteCommand> resendCommands = sRetainCommandMap.get(process);
            if (resendCommands == null) {
                synchronized (RemoteBridge.class) {
                    resendCommands = sRetainCommandMap.get(process);
                    if (resendCommands == null) {
                        resendCommands = Collections.newSetFromMap(new ConcurrentHashMap<RemoteCommand, Boolean>());
                        sRetainCommandMap.put(process, resendCommands);
                    }
                }
            }
            if (!resendCommands.contains(command)) {
                synchronized (this) {
                    if (!resendCommands.contains(command)) {
                        resendCommands.add(command);
                        if (lifecycle != null) {
                            final String finalProcess = process;
                            lifecycle.addObserver(new LifecycleObserver() {
                                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                                public void onDestroy() {
                                    Set<RemoteCommand> commands = sRetainCommandMap.get(finalProcess);
                                    if (commands != null) {
                                        commands.remove(command);
                                        RouterLogger.getCoreLogger().w(
                                                "[Client] remove resend command \"%s\"", command);
                                    }
                                    lifecycle.removeObserver(this);
                                }
                            });
                        }
                    }
                }
                RouterLogger.getCoreLogger().w(
                        "[Client] retain resend command \"%s\", with current lifecycle: %s",
                        command, lifecycle != null ? lifecycle.getCurrentState() : null);
            }
        }
    }

    private void registerBroadcast(String process) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String process = intent.getStringExtra(FIELD_REMOTE_LAUNCH_ACTION);
                RouterLogger.getCoreLogger().w(
                        "receive broadcast remote app launcher process: \"%s\"", process);
                resendRemoteCommand(process);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_ACTION + process);
        DRouter.getContext().registerReceiver(receiver, filter);
    }

    private static void resendRemoteCommand(String process) {
        Set<RemoteCommand> commands = sRetainCommandMap.get(process);
        if (commands != null) {
            for (RemoteCommand command : commands) {
                RouterLogger.getCoreLogger().w("execute resend command: \"%s\"", command);
                command.bridge.execute(command);
            }
        }
    }

    /**
     * This method can get remote binder after execute one remote command
     */
    public static IBinder getHostBinder(String authority) {
        IHostService service = RemoteProvider.getHostService(authority);
        return service != null ? service.asBinder() : null;
    }

}
