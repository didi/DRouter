package com.didi.drouter.generator

import com.didi.drouter.plugin.RouterSetting.Parse
import com.didi.drouter.utils.Logger
import javassist.ClassPool
import javassist.CtClass
import java.io.File

/**
 * Created by gaowei on 2018/8/30
 */
class ClassClassify(pool: ClassPool, setting: Parse) {
    private val classifies: MutableList<AbsRouterCollect> = ArrayList()

    init {
        classifies.add(RouterCollect(pool, setting))
        classifies.add(ServiceCollect(pool, setting))
        classifies.add(InterceptorCollect(pool, setting))
    }

    fun doClassify(ct: CtClass): Boolean {
        var take = false
        for (i in classifies.indices) {
            val cf = classifies[i]
            take = cf.collect(ct) || take
        }
        if (take) {
            Logger.d("    == router class: " + ct.name)
        }
        return take
    }

    @Throws(Exception::class)
    fun generatorRouter(routerDir: File) {
        for (i in classifies.indices) {
            val cf = classifies[i]
            cf.generate(routerDir)
        }
    }
}
