package com.didi.demo.service;

import com.didi.drouter.annotation.Service;
import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.service.ICallService;
import com.didi.drouter.service.ICallService2;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/16
 */
@Service(function = ICallService.class, alias = "login")
public class ServiceCallTest implements ICallService2<ParamObject, Integer, String> {

    @Override
    public String call(ParamObject argumentBean, Integer i) {
        RouterLogger.getAppLogger().d("ServiceCallTest call");
        RouterLogger.toast("ServiceCallTest call success");
        return null;
    }
}
