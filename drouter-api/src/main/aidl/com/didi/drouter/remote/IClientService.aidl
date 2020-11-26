// IClientService.aidl
package com.didi.drouter.remote;

// Declare any non-default types here with import statements
import com.didi.drouter.remote.RemoteCommand;
import com.didi.drouter.remote.RemoteResult;

/**
 * Created by gaowei on 2018/10/25
 */
interface IClientService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    RemoteResult callback(in RemoteCommand command);
}
