package com.didi.demo.service;

import com.didi.drouter.annotation.Service;
import com.didi.drouter.module_base.service.IServiceTest;
import com.didi.drouter.module_base.service.IServiceTest2;
import com.didi.drouter.service.AnyAbility;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2019/2/27
 */
@Service(function = AnyAbility.class, priority = 7, alias = "name1")
public class ServiceAny implements IServiceTest, IServiceTest2 {

    @Override
    public void test() {
        RouterLogger.toast("执行ServiceAny test成功");
    }

    @Override
    public void test2() {
        RouterLogger.toast("执行ServiceAny test2成功");
    }
}
