package com.didi.drouter.remote;

import android.os.RemoteException;

/**
 * Created by gaowei on 2019/2/27
 *
 * IRemoteCallback in server can be stored and reused indefinitely.
 */
public interface IRemoteCallback {

    /**
     * @param data the data of return to client.
     * @throws RemoteException the exception to throw in server if client is dead.
     */
    void callback(Object... data) throws RemoteException;
}
