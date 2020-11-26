package com.didi.drouter.router;

import android.support.annotation.NonNull;

/**
 * Created by gaowei on 2019/1/8
 */
public interface IRouterHandler {

    void handle(@NonNull Request request, @NonNull Result result);
}
