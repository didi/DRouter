// IHostService.aidl
package com.didi.drouter.remote;

// Declare any non-default types here with import statements
import com.didi.drouter.remote.RemoteCommand;
import com.didi.drouter.remote.RemoteResult;
import com.didi.drouter.remote.IClientService;

/**
 * Created by gaowei on 2018/10/25
 */
interface IHostService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    RemoteResult execute(in RemoteCommand command);
}
