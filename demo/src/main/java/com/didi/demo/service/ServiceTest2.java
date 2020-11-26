package com.didi.demo.service;

import android.content.Context;

import com.didi.drouter.annotation.Assign;
import com.didi.drouter.annotation.Service;
import com.didi.drouter.module_base.service.IServiceTest;
import com.didi.drouter.module_base.service.ServiceFeature;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/16
 */
@Service(function = IServiceTest.class,
        alias = {"test2"},
        feature = ServiceFeature.class)
public class ServiceTest2 implements IServiceTest {

    @Assign
    public static final int a = 1;
    @Assign(name = "a1")
    public static final int[] aaaa = {1, 2, 3};
    @Assign
    public static final short[] b = {1, 2, 3};
    @Assign
    public static final long[] c = {1, 2, 3};
    @Assign
    public static final byte[] d = {1, 2, 3};
    @Assign
    public static final char[] e = {'1', '2', '3'};   //必须带单引号
    @Assign
    public static final char e1 = '1';
    @Assign
    public static final float[] f = {1.0f, 2.0f, 3.0f};
    @Assign
    public static final double[] g = {1.0, 2.0, 3.0};
    @Assign
    public static final boolean[] h = {true, false};

    @Assign
    public static final String i = "1";
    @Assign
    public static final String[] j = {"1", "2", null};

    public ServiceTest2() {
        RouterLogger.getAppLogger().d("ServiceTest2 contructor");
    }

    public ServiceTest2(Integer type, String str, Boolean r) {
        RouterLogger.getAppLogger().d("ServiceTest2 argument contructor");
    }

    public ServiceTest2(int type, String str, boolean[] r, Context context) {
        RouterLogger.getAppLogger().d("ServiceTest2 argument contructor");
    }

    @Override
    public void test() {
        RouterLogger.getAppLogger().d("ServiceTest2 test");
        RouterLogger.toast("获取带有过滤器和带参构造器的Service成功");
    }
}
