// IHostService.aidl
package com.didi.drouter.remote;

// Declare any non-default types here with import statements
import com.didi.drouter.remote.StreamCmd;
import com.didi.drouter.remote.StreamResult;

/**
 * Created by gaowei on 2018/10/25
 */
interface IHostService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    StreamResult call(in StreamCmd command);

    oneway void callAsync(in StreamCmd command);
}
