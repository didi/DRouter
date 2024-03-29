package com.didi.drouter.remote;

import static com.didi.drouter.remote.RemoteProvider.BROADCAST_ACTION;
import static com.didi.drouter.remote.RemoteProvider.FIELD_REMOTE_LAUNCH_ACTION;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Strategy;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2022/01/25
 */
class CmdResend {

    // key is process
    private static final Map<String, Set<StreamCmd>> sRetainCommandMap = new ConcurrentHashMap<>();

    static void tryPrepareResend(RemoteBridge bridge, final StreamCmd command) {
        if (bridge.strategy.resend == Strategy.Resend.WAIT_ALIVE) {
            final String process = RemoteProvider.getProcess(bridge.strategy.authority);
            if (TextUtils.isEmpty(process)) {
                RouterLogger.getCoreLogger().e("[Client] retain command fail, for process name is null");
                return;
            }
            tryRegisterBroadcast(process);
            // If lifecycle exists, resend command can be removed when destroyed
            // check lifecycle state
            final Lifecycle lifecycle = bridge.lifecycle;
            if (lifecycle != null && lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
                RouterLogger.getCoreLogger().e("[Client] retain command fail, for lifecycle is assigned but destroyed");
                return;
            }
            Set<StreamCmd> resendCommands = sRetainCommandMap.get(process);
            if (resendCommands == null) {
                synchronized (RemoteBridge.class) {
                    resendCommands = sRetainCommandMap.get(process);
                    if (resendCommands == null) {
                        resendCommands = Collections.newSetFromMap(new ConcurrentHashMap<StreamCmd, Boolean>());
                        sRetainCommandMap.put(process, resendCommands);
                    }
                }
            }
            if (!resendCommands.contains(command)) {
                synchronized (CmdResend.class) {
                    if (!resendCommands.contains(command)) {
                        resendCommands.add(command);
                        if (lifecycle != null) {
                            RouterExecutor.main(() -> lifecycle.addObserver((LifecycleEventObserver) (source, event) -> {
                                if (event == Lifecycle.Event.ON_DESTROY) {
                                    Set<StreamCmd> commands = sRetainCommandMap.get(process);
                                    if (commands != null) {
                                        commands.remove(command);
                                        RouterLogger.getCoreLogger().w(
                                                "[Client] remove resend command \"%s\"", command);
                                    }
                                }
                            }));
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
            Set<StreamCmd> commands = sRetainCommandMap.get(process);
            if (commands != null) {
                for (StreamCmd command : commands) {
                    RouterLogger.getCoreLogger().w("execute resend command: \"%s\"", command);
                    command.bridge.execute(command);
                }
            }
        }
    }

}
