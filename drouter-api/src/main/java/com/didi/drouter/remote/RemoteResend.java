package com.didi.drouter.remote;

import static com.didi.drouter.remote.RemoteProvider.BROADCAST_ACTION;
import static com.didi.drouter.remote.RemoteProvider.FIELD_REMOTE_LAUNCH_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2022/01/25
 */
class RemoteResend {

    // key is process
    private static final Map<String, Set<RemoteCommand>> sRetainCommandMap = new ConcurrentHashMap<>();

    static void tryPrepareResend(RemoteBridge bridge, final RemoteCommand command) {
        if (bridge.strategy.resend == Extend.Resend.WAIT_ALIVE) {
            final String process = RemoteProvider.getProcess(bridge.strategy.authority);
            if (TextUtils.isEmpty(process)) {
                RouterLogger.getCoreLogger().e("[Client] retain command fail, for process name is null");
                return;
            }
            tryRegisterBroadcast(process);
            // If lifecycle exists, resend command can be removed when destroyed
            // check lifecycle state
            LifecycleOwner owner;
            final Lifecycle lifecycle = bridge.lifecycle != null ?
                    ((owner = bridge.lifecycle.get()) != null ? owner.getLifecycle() : null) : null;
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
                synchronized (RemoteResend.class) {
                    if (!resendCommands.contains(command)) {
                        resendCommands.add(command);
                        if (lifecycle != null) {
                            lifecycle.addObserver(new LifecycleEventObserver() {
                                @Override
                                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                                    if (event == Lifecycle.Event.ON_DESTROY) {
                                        Set<RemoteCommand> commands = sRetainCommandMap.get(process);
                                        if (commands != null) {
                                            commands.remove(command);
                                            RouterLogger.getCoreLogger().w(
                                                    "[Client] remove resend command \"%s\"", command);
                                        }
                                    }
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

    private static void tryRegisterBroadcast(String process) {
        if (!sRetainCommandMap.containsKey(process)) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BROADCAST_ACTION + process);
            DRouter.getContext().registerReceiver(new Broadcast(), filter);
        }
    }

    private static class Broadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String process = intent.getStringExtra(FIELD_REMOTE_LAUNCH_ACTION);
            RouterLogger.getCoreLogger().w(
                    "receive broadcast remote app launcher process: \"%s\"", process);
            Set<RemoteCommand> commands = sRetainCommandMap.get(process);
            if (commands != null) {
                for (RemoteCommand command : commands) {
                    RouterLogger.getCoreLogger().w("execute resend command: \"%s\"", command);
                    command.bridge.execute(command);
                }
            }
        }
    }

}
