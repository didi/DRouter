package com.didi.drouter.router;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2019/1/8
 */
public interface IRouterHandler {

    void handle(@NonNull Request request, @NonNull Result result);
}
