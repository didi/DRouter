package com.didi.drouter.loader.host;

import com.didi.drouter.store.MetaLoader;

import java.util.Map;

/**
 * Created by gaowei on 2020/10/25
 */
public class ServiceLoader extends MetaLoader {

    @Override
    public void load(Map<?, ?> data) {
        throw new RuntimeException();
    }
}
