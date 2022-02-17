package com.didi.drouter.remote;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Created by gaowei on 2018/10/23
 */
class StreamCmd implements Parcelable {

    // ignore, client
    RemoteBridge bridge;

    Class<?> serviceClass;
    String alias;
    Object feature;
    String methodName;
    @Nullable Object[] constructorArgs;
    @Nullable Object[] methodArgs;

    StreamCmd() {
    }

    private StreamCmd(Parcel in) {
        serviceClass = (Class<?>) in.readSerializable();
        alias = in.readString();
        methodName = in.readString();
        feature = StreamTransfer.reverse(in.readValue(getClass().getClassLoader()));
        constructorArgs = (Object[]) StreamTransfer.reverse(in.readValue(getClass().getClassLoader()));
        methodArgs = (Object[]) StreamTransfer.reverse(in.readValue(getClass().getClassLoader()));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(serviceClass);
        dest.writeString(alias);
        dest.writeString(methodName);
        dest.writeValue(StreamTransfer.transform(feature));
        dest.writeValue(StreamTransfer.transform(constructorArgs));
        dest.writeValue(StreamTransfer.transform(methodArgs));
    }

    public static final Parcelable.Creator<StreamCmd> CREATOR = new Parcelable.Creator<StreamCmd>() {
        public StreamCmd createFromParcel(Parcel in) {
            return new StreamCmd(in);
        }
        public StreamCmd[] newArray(int size) {
            return new StreamCmd[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StreamCmd)) return false;
        StreamCmd command = (StreamCmd) o;
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
