package com.didi.drouter.generator

import com.didi.drouter.annotation.Interceptor
import com.didi.drouter.plugin.RouterSetting.Parse
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.StoreUtil
import com.didi.drouter.utils.TextUtil
import javassist.ClassPool
import javassist.CtClass
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by gaowei on 2018/8/30
 */
internal class InterceptorCollect(pool: ClassPool, setting: Parse) :
    AbsRouterCollect(pool, setting) {
    private val interceptorClass: MutableMap<String, CtClass> = ConcurrentHashMap()
    private val items: MutableList<String> = ArrayList()
    override fun collect(ct: CtClass): Boolean {
        if (include(ct)) {
            interceptorClass[ct.name] = ct
            return true
        }
        return false
    }

    @Throws(Exception::class)
    override fun generate(routerDir: File) {
        val ctClass = pool.makeClass("$packageName.InterceptorLoader")
        val superClass = pool["com.didi.drouter.store.MetaLoader"]
        ctClass.superclass = superClass
        val builder = StringBuilder()
        builder.append("public void load(java.util.Map data) {\n")
        for (interceptorCc in interceptorClass.values) {
            try {
                if (isNonStaticInnerClass(interceptorCc)) {
                    throw Exception("Annotation can not use non static inner class")
                }
                if (!checkSuper(interceptorCc, "com.didi.drouter.router.IRouterInterceptor")) {
                    throw Exception("@Interceptor class does not implement IRouterInterceptor interface")
                }
                val interceptor = interceptorCc.getAnnotation(
                    Interceptor::class.java
                ) as Interceptor
                var proxyCc: CtClass? = null
                val constructor = interceptorCc.getDeclaredConstructor(null)
                if (constructor != null) {
                    val proxyInterface = pool["com.didi.drouter.store.IRouterProxy"]
                    proxyCc = pool.makeClass(
                        AbsRouterCollect.Companion.PROXY +
                                interceptorCc.name.replace(".", "_")
                    )
                    proxyCc.addInterface(proxyInterface)
                    val method1 = String.format(
                        "public java.lang.Object newInstance(android.content.Context context) {" +
                                "{  return new %s();} }",
                        interceptorCc.name
                    )
                    generatorClass(
                        routerDir,
                        proxyCc,
                        method1,
                        METHOD2
                    )
                }

                // class is the key
                val itemBuilder = StringBuilder()
                itemBuilder.append("    data.put(")
                itemBuilder.append(interceptorCc.name)
                itemBuilder.append(".class")
                itemBuilder.append(", com.didi.drouter.store.RouterMeta.build(")
                itemBuilder.append("com.didi.drouter.store.RouterMeta.INTERCEPTOR)")
                itemBuilder.append(".assembleInterceptor(")
                itemBuilder.append(interceptorCc.name)
                itemBuilder.append(".class, ")
                itemBuilder.append(if (proxyCc != null) "new " + proxyCc.name + "()" else "null")
                itemBuilder.append(",")
                itemBuilder.append(interceptor.priority)
                itemBuilder.append(",")
                itemBuilder.append(interceptor.global)
                itemBuilder.append(",")
                itemBuilder.append(interceptor.cache)
                itemBuilder.append("));\n")
                val name = interceptor.name
                if (!TextUtil.isEmpty(name)) {
                    // name is the key
                    itemBuilder.append("    data.put(\"")
                    itemBuilder.append(name)
                    itemBuilder.append("\", com.didi.drouter.store.RouterMeta.build(")
                    itemBuilder.append("com.didi.drouter.store.RouterMeta.INTERCEPTOR)")
                    itemBuilder.append(".assembleInterceptor(")
                    itemBuilder.append(interceptorCc.name)
                    itemBuilder.append(".class, ")
                    itemBuilder.append(if (proxyCc != null) "new " + proxyCc.name + "()" else "null")
                    itemBuilder.append(",")
                    itemBuilder.append(interceptor.priority)
                    itemBuilder.append(",")
                    itemBuilder.append(interceptor.global)
                    itemBuilder.append(",")
                    itemBuilder.append(interceptor.cache)
                    itemBuilder.append("));\n")
                    val duplicate = StoreUtil.insertUri(name, interceptorCc)
                    if (duplicate != null) {
                        throw Exception(
                            """"name=$name" on ${interceptorCc.name}
has duplication of name with class: $duplicate"""
                        )
                    }
                }
                items.add(itemBuilder.toString())
            } catch (e: Exception) {
                throw Exception(
                    """Class: === ${interceptorCc.name} ===
Cause: ${e.message}""", e
                )
            }
        }
        items.sort()
        for (item in items) {
            builder.append(item)
        }
        builder.append("}")
        Logger.d("\nclass InterceptorLoader\n$builder")
        generatorClass(routerDir, ctClass, builder.toString())
    }

    override fun include(superCt: CtClass): Boolean {
        return superCt.hasAnnotation(Interceptor::class.java)
    }
}
