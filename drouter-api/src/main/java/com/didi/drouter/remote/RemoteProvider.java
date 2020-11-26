package com.didi.drouter.remote;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.SystemUtil;

/**
 * Created by gaowei on 2019/1/24
 */
public class RemoteProvider extends ContentProvider {

    static final String FIELD_REMOTE_BINDER = "field_remote_binder";
    static final String FIELD_REMOTE_PROCESS = "field_remote_process";
    static final String FIELD_REMOTE_LAUNCH_ACTION = "field_remote_launch_action";
    static final String BROADCAST_ACTION = "drouter.process.action.";
    private static boolean hasSendBroadcast;

    private static final IHostService.Stub stub = new IHostService.Stub() {

        @Override
        public RemoteResult execute(RemoteCommand command) {
            try {
                return new RemoteDispatcher().execute(command);
            } catch (RuntimeException e) {
                RouterLogger.getCoreLogger().e("[Service] exception: %s", e);
                throw e;  // will not crash
            }
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
                    String.format("[%s] onCreate | Context: %s | Process: \"%s\"" ,
                            getClass().getSimpleName(), getContext(), process));
        }
        return true;
    }

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        RouterLogger.getCoreLogger().d(
                "[%s] is called by client to get binder, process: \"%s\"",
                getClass().getSimpleName(), SystemUtil.getProcessName());
        Bundle bundle = new Bundle();
        bundle.putParcelable(FIELD_REMOTE_BINDER, new BinderParcel(stub));
        bundle.putString(FIELD_REMOTE_PROCESS, SystemUtil.getProcessName());
        return bundle;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
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
