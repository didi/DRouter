package com.didi.drouter.remote;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by gaowei on 2018/10/23
 */
class RemoteResult implements Parcelable {

    static final String EXECUTING = "executing";
    static final String SUCCESS = "success";
    static final String FAIL = "fail";

    String state;
    Object result;

    RemoteResult(String state) {
        this.state = state;
    }

    RemoteResult(Parcel in) {
        state = in.readString();
        result = RemoteStream.reverse(in.readValue(getClass().getClassLoader()));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(state);
        dest.writeValue(RemoteStream.transform(result));
    }

    public static final Creator<RemoteResult> CREATOR = new Creator<RemoteResult>() {
        public RemoteResult createFromParcel(Parcel in) {
            return new RemoteResult(in);
        }

        public RemoteResult[] newArray(int size) {
            return new RemoteResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
