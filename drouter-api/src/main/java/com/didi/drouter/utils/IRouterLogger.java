package com.didi.drouter.utils;

/**
 * Created by gaowei on 2018/10/19
 */
public interface IRouterLogger {

    void d(String TAG, String content);
    void w(String TAG, String content);
    void e(String TAG, String content);
}
