package com.didi.drouter.module_base.remote;

import android.content.Context;
import android.os.SharedMemory;

import androidx.annotation.Keep;

import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.ResultObject;
import com.didi.drouter.remote.IRemoteCallback;

import java.util.List;

/**
 * Created by gaowei on 2018/11/2
 */
@Keep
public interface IRemoteFunction {

    ResultObject handle(ParamObject[] x, ParamObject y, Integer z, Context context, IRemoteCallback.Type2<String, Integer> callback);

    void trans(List<SharedMemory> memory);

    void register(IRemoteCallback.Type0 callback);

    void unregister(IRemoteCallback.Type0 callback);

    void kill();

    void call();

}
