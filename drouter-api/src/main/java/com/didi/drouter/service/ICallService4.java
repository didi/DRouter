package com.didi.drouter.service;

/**
 * Created by gaowei on 2019/3/28
 */
public interface ICallService4<Param1, Param2, Param3, Param4, Result> {

    Result call(Param1 param1, Param2 param2, Param3 param3, Param4 param4);
}
