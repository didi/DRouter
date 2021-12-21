package com.didi.drouter.module_base.remote;

import android.content.Context;
import android.support.annotation.Keep;

import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.ResultObject;
import com.didi.drouter.remote.IRemoteCallback;

/**
 * Created by gaowei on 2018/11/2
 */
@Keep
public interface IRemoteFunction {

    ResultObject handle(ParamObject[] x, ParamObject y, Integer z, Context context, IRemoteCallback callback);

    void register(IRemoteCallback callback);

    void unregister(IRemoteCallback callback);

    void kill();

    void call();

}
