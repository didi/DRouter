package com.didi.drouter.remote;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by gaowei on 2018/10/23
 */
class StreamResult implements Parcelable {

    static final String EXECUTING = "executing";
    static final String SUCCESS = "success";
    static final String FAIL = "fail";

    String state;
    Object result;

    StreamResult(String state) {
        this.state = state;
    }

    StreamResult(Parcel in) {
        state = in.readString();
        result = StreamTransfer.reverse(in.readValue(getClass().getClassLoader()));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(state);
        dest.writeValue(StreamTransfer.transform(result));
    }

    public static final Creator<StreamResult> CREATOR = new Creator<StreamResult>() {
        public StreamResult createFromParcel(Parcel in) {
            return new StreamResult(in);
        }
        public StreamResult[] newArray(int size) {
            return new StreamResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

}
