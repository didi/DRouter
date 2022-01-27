package com.didi.drouter.remote;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Created by gaowei on 2018/10/23
 */
class RemoteCommand implements Parcelable {

    // ignore
    RemoteBridge bridge;

    Class<?> serviceClass;
    String alias;
    Object feature;
    String methodName;
    @Nullable Object[] constructorArgs;
    @Nullable Object[] methodArgs;

    RemoteCommand() {
    }

    private RemoteCommand(Parcel in) {
        serviceClass = (Class<?>) in.readSerializable();
        alias = in.readString();
        feature = DataStream.reverse(in.readValue(getClass().getClassLoader()));
        methodName = in.readString();
        constructorArgs = (Object[]) DataStream.reverse(in.readValue(getClass().getClassLoader()));
        methodArgs = (Object[]) DataStream.reverse(in.readValue(getClass().getClassLoader()));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(serviceClass);
        dest.writeString(alias);
        dest.writeValue(DataStream.transform(feature));
        dest.writeString(methodName);
        dest.writeValue(DataStream.transform(constructorArgs));
        dest.writeValue(DataStream.transform(methodArgs));
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
        return  equals(serviceClass, command.serviceClass) &&
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
        return Arrays.hashCode(new Object[]{serviceClass, alias, feature, methodName, bridge});
    }

    @NonNull
    @Override
    public String toString() {
        if (serviceClass != null) {
            return "service:" + serviceClass.getSimpleName() + " methodName:" + methodName;
        } else {
            return "service callback";
        }
    }
}
