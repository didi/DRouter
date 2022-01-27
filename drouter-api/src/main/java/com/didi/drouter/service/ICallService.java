package com.didi.drouter.service;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2019/3/28
 */
public interface ICallService {

    <T> T call(Object... params);

    interface Type0<Result> {
        Result call();
    }
    interface Type1<Param1, Result> {
        Result call(Param1 param1);
    }
    interface Type2<Param1, Param2, Result> {
        Result call(Param1 param1, Param2 param2);
    }
    interface Type3<Param1, Param2, Param3, Result> {
        Result call(Param1 param1, Param2 param2, Param3 param3);
    }
    interface Type4<Param1, Param2, Param3, Param4, Result> {
        Result call(Param1 param1, Param2 param2, Param3 param3, Param4 param4);
    }
    interface Type5<Param1, Param2, Param3, Param4, Param5, Result> {
        Result call(Param1 param1, Param2 param2, Param3 param3, Param4 param4, Param5 param5);
    }
    interface TypeN<Result> {
        Result call(@NonNull Object... params);
    }
}
