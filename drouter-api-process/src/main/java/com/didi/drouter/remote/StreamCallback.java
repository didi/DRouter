package com.didi.drouter.remote;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class StreamCallback implements Parcelable {

    // key is Binder, key will be recycled after binder-proxy recycle
    static final Map<WeakReference<IHostService>, Listener> clientStubPool = new ConcurrentHashMap<>();
    // key is BinderProxy
    static final List<WeakReference<IRemoteCallback.Base>> serverProxyPool = new CopyOnWriteArrayList<>();
    // for client and server, Binder/BinderProxy
    IHostService binder;
    int type;

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        String b = "Server";
        if (binder instanceof HostStub) b = "Client";
        RouterLogger.getCoreLogger().w("[%s] StreamCallback recycle", b);
    }

    static class HostStub extends IHostService.Stub {
        IRemoteCallback.Base clientCallback;
        HostStub(IRemoteCallback.Base clientCallback) {
            this.clientCallback = clientCallback;
        }

        @Override
        public StreamResult call(StreamCmd command) {
            return null;
        }

        @Override
        public void callAsync(final StreamCmd command) {
            final IRemoteCallback.Base callback = clientCallback;
            RouterLogger.getCoreLogger().dw("[Client] receive callback \"%s\" from binder thread %s",
                    callback == null, callback, Thread.currentThread().getName());
            if (callback != null) {
                RouterExecutor.execute(callback.thread(), () -> callback.callback(command.methodArgs));
            }
        }

        @Override
        protected void finalize() throws Throwable {
            RouterLogger.getCoreLogger().w("[Client] IHostService.Stub recycle");
            super.finalize();
        }
    }

    // client
    // HostStub -> realCallback
    StreamCallback(Object callback) {
        IRemoteCallback.Base clientCallback = (IRemoteCallback.Base) callback;
        type = getType(clientCallback);
        for (Map.Entry<WeakReference<IHostService>, Listener> weakRef : clientStubPool.entrySet()) {
            IHostService stubRef = weakRef.getKey().get();
            if (stubRef == null) {
                Listener listener = clientStubPool.remove(weakRef.getKey());
                unregister(listener);
            } else if (((HostStub) stubRef).clientCallback == clientCallback) {
                binder = stubRef;
            }
        }
        if (binder != null) {
            return;
        }
        binder = new HostStub(clientCallback);
        WeakReference<IHostService> weakRef = new WeakReference<>(binder);
        clientStubPool.put(weakRef, register(clientCallback, weakRef));
    }

    static class Listener {
        Lifecycle lifecycle;
        CallbackLifeObserver lifeObserver;
        IBinder binderProxy;
        DeathRecipient deathRecipient;
    }

    // server
    // realCallback -> StreamCallback -> binderProxy -> linkToDeath
    // realCallback -> binderProxy -> linkToDeath
    Object getCallback() {
        IRemoteCallback.Base realCallback = null;
        for (WeakReference<IRemoteCallback.Base> weakRef : serverProxyPool) {
            IRemoteCallback.Base callback = weakRef.get();
            if (callback == null) {
                serverProxyPool.remove(weakRef);
            } else if (callback.binder == binder.asBinder()) {
                realCallback = callback;
            }
        }
        if (realCallback != null) {
            return realCallback;
        }
        realCallback = createRealCallback(type, binder.asBinder());
        serverProxyPool.add(new WeakReference<>(realCallback));
        return realCallback;
    }

    private static int getType(IRemoteCallback.Base object) {
        if (object instanceof IRemoteCallback.Type0) {
            return 0;
        } else if (object instanceof IRemoteCallback.Type1) {
            return 1;
        } else if (object instanceof IRemoteCallback.Type2) {
            return 2;
        } else if (object instanceof IRemoteCallback.Type3) {
            return 3;
        } else if (object instanceof IRemoteCallback.Type4) {
            return 4;
        } else if (object instanceof IRemoteCallback.Type5) {
            return 5;
        } else if (object instanceof IRemoteCallback.TypeN) {
            return -1;
        }
        throw new RuntimeException("[Client] callback type error");
    }

    private void callbackToClient(Object... data) {
        RouterLogger.getCoreLogger().w("[Server] start remote callback");
        StreamCmd callbackCommand = new StreamCmd();
        callbackCommand.methodArgs = data;
        try {
            binder.callAsync(callbackCommand);
        } catch (RemoteException e) {
            RouterLogger.getCoreLogger().e("[Server] IRemoteCallback invoke Exception %s", e);
        }
    }

    private IRemoteCallback.Base createRealCallback(int type, IBinder binder) {
        IRemoteCallback.Base realCallback;
        if (type == 0) {
            realCallback = new IRemoteCallback.Type0() {
                @Override
                public void callback() {
                    callbackToClient();
                }
            };
        } else if (type == 1) {
            realCallback = new IRemoteCallback.Type1<Object>() {
                @Override
                public void callback(Object p1) {
                    callbackToClient(p1);
                }
            };
        } else if (type == 2) {
            realCallback = new IRemoteCallback.Type2<Object, Object>() {
                @Override
                public void callback(Object p1, Object p2) {
                    callbackToClient(p1, p2);
                }
            };
        } else if (type == 3) {
            realCallback = new IRemoteCallback.Type3<Object, Object, Object>() {
                @Override
                public void callback(Object p1, Object p2, Object p3) {
                    callbackToClient(p1, p2, p3);
                }
            };
        } else if (type == 4) {
            realCallback = new IRemoteCallback.Type4<Object, Object, Object, Object>() {
                @Override
                public void callback(Object p1, Object p2, Object p3, Object p4) {
                    callbackToClient(p1, p2, p3, p4);
                }
            };
        } else if (type == 5) {
            realCallback = new IRemoteCallback.Type5<Object, Object, Object, Object, Object>() {
                @Override
                public void callback(Object p1, Object p2, Object p3, Object p4, Object p5) {
                    callbackToClient(p1, p2, p3, p4, p5);
                }
            };
        } else if (type == -1) {
            realCallback = new IRemoteCallback.TypeN() {
                @Override
                public void callback(Object... data) {
                    callbackToClient(data);
                }
            };
        } else {
            throw new RuntimeException("[Server] callback type error");
        }
        realCallback.binder = binder;
        return realCallback;
    }

    static class CallbackLifeObserver implements LifecycleEventObserver {
        WeakReference<IHostService> weakRef;
        CallbackLifeObserver(WeakReference<IHostService> weakRef) {
            this.weakRef = weakRef;
        }
        @Override
        public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                IHostService stub = weakRef.get();
                if (stub != null) {
                    ((HostStub)stub).clientCallback = null;
                }
            }
        }
    }

    static class DeathRecipient implements IBinder.DeathRecipient {
        WeakReference<IHostService> weakRef;
        DeathRecipient(WeakReference<IHostService> weakRef) {
            this.weakRef = weakRef;
        }
        @Override
        public void binderDied() {
            IHostService stub = weakRef.get();
            if (stub != null) {
                IRemoteCallback.Base callback = ((HostStub)stub).clientCallback;
                if (callback != null) {
                    callback.onServerDead();
                }
            }
        }
    }

    private Listener register(final IRemoteCallback.Base clientCallback, WeakReference<IHostService> weakRef) {
        final Listener listener = new Listener();
        Lifecycle lifecycle = clientCallback.lifecycle();
        if (lifecycle != null) {
            // early to release, avoid the impact of server delay recycle
            listener.lifecycle = lifecycle;
            listener.lifeObserver = new CallbackLifeObserver(weakRef);
            RouterExecutor.main(() -> clientCallback.lifecycle().addObserver(listener.lifeObserver));
        }
        IBinder binderProxy = RemoteBridge.getHostBinder(clientCallback.authority);
        if (binderProxy != null) {
            try {
                listener.binderProxy = binderProxy;
                listener.deathRecipient = new DeathRecipient(weakRef);
                binderProxy.linkToDeath(listener.deathRecipient, 0);
            } catch (RemoteException ignore) {
            }
        }
        return listener;
    }

    private void unregister(final Listener listener) {
        if (listener != null && listener.lifecycle != null) {
            RouterExecutor.main(() -> listener.lifecycle.removeObserver(listener.lifeObserver));
        }
        if (listener != null && listener.binderProxy != null) {
            listener.binderProxy.unlinkToDeath(listener.deathRecipient, 0);
        }
    }

    static void preprocess(Object[] args, String authority) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof IRemoteCallback.Base) {
                    ((IRemoteCallback.Base) arg).authority = authority;
                }
            }
        }
    }

    StreamCallback(Parcel in) {
        type = in.readInt();
        binder = IHostService.Stub.asInterface(in.readStrongBinder());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeStrongBinder(binder.asBinder());
    }

    public static final Creator<StreamCallback> CREATOR = new Creator<StreamCallback>() {
        @Override
        public StreamCallback createFromParcel(Parcel in) {
            return new StreamCallback(in);
        }

        @Override
        public StreamCallback[] newArray(int size) {
            return new StreamCallback[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
}
