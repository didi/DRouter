package com.didi.drouter.generator;

import com.didi.drouter.plugin.RouterSetting;
import com.didi.drouter.utils.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;

/**
 * Created by gaowei on 2018/8/30
 */
public class ClassClassify {

    private final List<AbsRouterCollect> classifies = new ArrayList<>();

    public ClassClassify(ClassPool pool, RouterSetting.Parse setting) {
        classifies.add(new RouterCollect(pool, setting));
        classifies.add(new ServiceCollect(pool, setting));
        classifies.add(new InterceptorCollect(pool, setting));
    }

    public boolean doClassify(CtClass ct) {
        boolean take = false;
        for (int i = 0; i < classifies.size(); i++) {
            AbsRouterCollect cf = classifies.get(i);
            take = cf.collect(ct) || take;
        }
        if (take) {
            Logger.d("    == router class: " + ct.getName());
        }
        return take;
    }

    public void generatorRouter(File routerDir) throws Exception {
        for (int i = 0; i < classifies.size(); i++) {
            AbsRouterCollect cf = classifies.get(i);
            cf.generate(routerDir);
        }
    }

}
