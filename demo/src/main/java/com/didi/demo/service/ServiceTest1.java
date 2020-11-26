package com.didi.demo.service;

import android.content.Context;

import com.didi.drouter.annotation.Service;
import com.didi.drouter.api.Extend;
import com.didi.drouter.module_base.service.IServiceTest;
import com.didi.drouter.module_base.service.ServiceFeature;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2019/2/27
 */
@Service(function = IServiceTest.class, alias = "test1", priority = -8, cache = Extend.Cache.WEAK)
public class ServiceTest1 implements IServiceTest {

    public ServiceTest1() {
        RouterLogger.getAppLogger().d("ServiceTest1 contructor");
    }

    public ServiceTest1(Object o) {
        RouterLogger.getAppLogger().d("ServiceTest1 one argument contructor");
    }

    public ServiceTest1(int type, String str, ServiceFeature r, Context context) {
        RouterLogger.getAppLogger().d("ServiceTest1 multi argument contructor");
    }

    @Override
    public void test() {
        RouterLogger.toast("执行ServiceTest1成功");
    }
}
