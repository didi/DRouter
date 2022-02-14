package com.didi.drouter.remote;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.SystemUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2019/1/24
 *
 * Defining your own ProviderName extend RemoteProvider is highly recommended.
 */
public class RemoteProvider extends ContentProvider {

    static final String FIELD_REMOTE_BINDER = "field_remote_binder";
    static final String FIELD_REMOTE_PROCESS = "field_remote_process";
    static final String FIELD_REMOTE_LAUNCH_ACTION = "field_remote_launch_action";
    static final String BROADCAST_ACTION = "drouter.process.action.";

    // ensure once in process lifecycle
    private static boolean hasSendBroadcast;

    // key is authority
    private static final Map<String, IHostService> sHostServiceMap = new ConcurrentHashMap<>();
    // key is authority, value is process
    private static final Map<String, String> sProcessMap = new ConcurrentHashMap<>();

    private static final IHostService.Stub stub = new IHostService.Stub() {
        @Override
        public RemoteResult call(RemoteCommand command) {
            try {
                return new RemoteDispatcher().call(command);
            } catch (RuntimeException e) {
                RouterLogger.getCoreLogger().e("[Server] exception: %s", e);
                throw e;  // will not crash
            }
        }
        @Override
        public void callAsync(RemoteCommand command) {
            call(command);
        }
    };

    @Override
    public boolean onCreate() {
        String process = SystemUtil.getProcessName();
        if (getContext() instanceof Application) {
            SystemUtil.setApplication((Application) getContext());
            RouterLogger.getCoreLogger().d(
                    "[%s] onCreate | Context: %s | Process: \"%s\"" ,
                    getClass().getSimpleName(), getContext(), process);
            if (!hasSendBroadcast) {
                Intent intent = new Intent(BROADCAST_ACTION + process);
                intent.putExtra(FIELD_REMOTE_LAUNCH_ACTION, process);
                getContext().sendBroadcast(intent);
                hasSendBroadcast = true;
            }
        } else {
            Log.e(RouterLogger.NAME,
                    String.format("[%s] onCreate multiProcess? | Context: %s | Process: \"%s\"" ,
                            getClass().getSimpleName(), getContext(), process));
        }
        return true;
    }

    @Nullable @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        RouterLogger.getCoreLogger().d(
                "[%s] is called by client to get binder, process: \"%s\"",
                getClass().getSimpleName(), SystemUtil.getProcessName());
        Bundle bundle = new Bundle();
        bundle.putParcelable(FIELD_REMOTE_BINDER, new BinderParcel(stub));
        bundle.putString(FIELD_REMOTE_PROCESS, SystemUtil.getProcessName());
        return bundle;
    }

    @Nullable @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }

    @Nullable @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    static IHostService getHostService(final String authority) {
        IHostService hostService = sHostServiceMap.get(authority);
        if (hostService != null) {
            return hostService;
        }
        try {
            synchronized (RemoteCommand.class) {
                hostService = sHostServiceMap.get(authority);
                if (hostService != null) {
                    return hostService;
                }
                Bundle bundle = null;
                for (int i = 0; i < 3; i++) {    // remote process killed case and retry, return null
                    try {
                        ContentProviderClient client = DRouter.getContext().getContentResolver()
                                .acquireUnstableContentProviderClient(authority);
                        if (client != null) {
                            bundle = client.call("", "", null);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                client.close();
                            } else {
                                client.release();
                            }
                        }
                    } catch (RuntimeException e) {
                        RouterLogger.getCoreLogger().e(
                                "[Client] getHostService call provider, try time %s, exception: %s", i, e.getMessage());
                    }
                    if (bundle != null) {
                        break;
                    }
                }
                if (bundle != null) {
                    bundle.setClassLoader(RemoteBridge.class.getClassLoader());
                    BinderParcel parcel = bundle.getParcelable(RemoteProvider.FIELD_REMOTE_BINDER);
                    sProcessMap.put(authority, bundle.getString(FIELD_REMOTE_PROCESS));
                    if (parcel != null) {
                        hostService = IHostService.Stub.asInterface(parcel.getBinder());
                        hostService.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                // rebinding is not required
                                // this may slow, so remove again when execute DeadObjectException
                                sHostServiceMap.remove(authority);
                                RouterLogger.getCoreLogger().e(
                                        "[Client] linkToDeath: remote \"%s\" is died", authority);
                            }
                        }, 0);
                        sHostServiceMap.put(authority, hostService);
                        RouterLogger.getCoreLogger().d("[Client] get server binder success from provider, authority \"%s\"", authority);
                        return hostService;
                    }
                }
            }
        } catch (RemoteException e) {      // linkToDeath
            RouterLogger.getCoreLogger().e("[Client] getHostService remote exception: %s", e);
        }
        return null;
    }

    static void removeHostService(String authority) {
        sHostServiceMap.remove(authority);
    }

    static String getProcess(String authority) {
        return sProcessMap.get(authority);
    }

    static class BinderParcel implements Parcelable {

        private final IBinder mBinder;

        BinderParcel(Parcel in) {
            mBinder = in.readStrongBinder();
        }

        BinderParcel(IBinder binder) {
            mBinder = binder;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStrongBinder(mBinder);
        }

        IBinder getBinder() {
            return mBinder;
        }

        public static final Creator<BinderParcel> CREATOR = new Creator<BinderParcel>() {
            @Override
            public BinderParcel createFromParcel(Parcel in) {
                return new BinderParcel(in);
            }

            @Override
            public BinderParcel[] newArray(int size) {
                return new BinderParcel[size];
            }
        };
    }

}
