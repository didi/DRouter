package com.didi.drouter.service;

import android.support.annotation.NonNull;

/**
 * Created by gaowei on 2019/3/28
 */
public interface ICallServiceN<Result> {

    Result call(@NonNull Object... params);
}
