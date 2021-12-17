package com.didi.drouter.module_base;

import com.didi.drouter.annotation.Service;
import com.didi.drouter.module_base.service.IServiceTest;

/**
 * Created by gaowei on 2021/12/5
 */
@Service(function = IServiceTest.class)
public class TestService implements IServiceTest {
    @Override
    public void test() {

    }
}
