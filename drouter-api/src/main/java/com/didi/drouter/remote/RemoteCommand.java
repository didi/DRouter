package com.didi.drouter.remote;

import android.arch.lifecycle.LifecycleOwner;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by gaowei on 2018/10/23
 */
class RemoteCommand implements Parcelable {

    static final int REQUEST = 0;
    static final int REQUEST_RESULT = 1;
    static final int SERVICE = 2;
    static final int SERVICE_CALLBACK = 3;

    private final int type;
    int resendStrategy;
    RemoteBridge bridge;
    WeakReference<LifecycleOwner> lifecycle;

    String uri;
    IBinder binder;
    boolean isActivityStarted;
    int routerSize;
    Bundle extra;
    Map<String, Object> addition;

    Class<?> serviceClass;
    String alias;
    Object feature;
    String methodName;
    @Nullable Object[] constructor;
    @Nullable Object[] parameters;
    //@Nullable Object[] callbackClass;
    Object[] callbackData;

    RemoteCommand(int type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    RemoteCommand(Parcel in) {
        type = in.readInt();
        if (type == REQUEST) {
            uri = in.readString();
            binder = in.readStrongBinder();
            extra = in.readBundle(getClass().getClassLoader());
            addition = (Map<String, Object>) RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
        }
        if (type == REQUEST_RESULT) {
            isActivityStarted = in.readInt() == 1;
            routerSize = in.readInt();
            extra = in.readBundle(getClass().getClassLoader());
            addition = (Map<String, Object>) RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
        }
        if (type == SERVICE) {
            serviceClass = (Class<?>) in.readSerializable();
            alias = in.readString();
            feature = RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
            methodName = in.readString();
            constructor = (Object[]) RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
            parameters = (Object[]) RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
            //callbackClass = (Class<?>[]) RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
        }
        if (type == SERVICE_CALLBACK) {
            callbackData = (Object[]) RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        if (type == REQUEST) {
            dest.writeString(uri);
            dest.writeStrongBinder(binder);
            dest.writeBundle(extra);
            dest.writeValue(RemoteStream.transform(addition));
        }
        if (type == REQUEST_RESULT) {
            dest.writeInt(isActivityStarted ? 1 : 0);
            dest.writeInt(routerSize);
            dest.writeBundle(extra);
            dest.writeValue(RemoteStream.transform(addition));
        }
        if (type == SERVICE) {
            dest.writeSerializable(serviceClass);
            dest.writeString(alias);
            dest.writeValue(RemoteStream.transform(feature));
            dest.writeString(methodName);
            dest.writeValue(RemoteStream.transform(constructor));
            dest.writeValue(RemoteStream.transform(parameters));
            //dest.writeValue(RemoteStream.transform(callbackClass));
        }
        if (type == SERVICE_CALLBACK) {
            dest.writeValue(RemoteStream.transform(callbackData));
        }
    }

    public static final Parcelable.Creator<RemoteCommand> CREATOR = new Parcelable.Creator<RemoteCommand>() {
        public RemoteCommand createFromParcel(Parcel in) {
            return new RemoteCommand(in);
        }
        public RemoteCommand[] newArray(int size) {
            return new RemoteCommand[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RemoteCommand)) return false;
        RemoteCommand command = (RemoteCommand) o;
        return type == command.type &&
                equals(uri, command.uri) &&
                equals(serviceClass, command.serviceClass) &&
                equals(alias, command.alias) &&
                equals(feature, command.feature) &&
                equals(methodName, command.methodName) &&
                equals(bridge, command.bridge);    // belong to the same build instance and the same method
    }

    private static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    @Override
    public int hashCode() {
        // Two instance has different hash code by default.
        return Arrays.hashCode(new Object[]{type, uri, serviceClass, alias, feature, methodName, bridge});
    }

    @Override
    public String toString() {
        if (type == REQUEST) {
            return "request uri: " + uri;
        } else if (type == REQUEST_RESULT) {
            return "request result";
        } else if (type == SERVICE) {
            return "service:" + serviceClass.getSimpleName() + " methodName:" + methodName;
        } else if (type == SERVICE_CALLBACK) {
            return "service_callback";
        }
        return super.toString();
    }
}
