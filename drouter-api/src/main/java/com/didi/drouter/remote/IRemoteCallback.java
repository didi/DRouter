package com.didi.drouter.remote;

import android.os.RemoteException;

/**
 * Created by gaowei on 2019/2/27
 */
public interface IRemoteCallback {

    void callback(Object... data) throws RemoteException;
}
