package com.didi.drouter.utils

import javassist.CtClass

/**
 * Created by gaowei on 2018/10/30
 */
object StoreUtil {
    private val pathMap: MutableMap<String, String> = HashMap()
    private val classMap: MutableMap<String, Class<*>> = HashMap()
    private val callAliasMap: MutableMap<String, String> = HashMap()
    fun insertUri(uri: String, cc: CtClass): String? {
        if (!pathMap.containsKey(uri)) {
            pathMap[uri] = cc.name
            return null
        }
        return pathMap[uri]
    }

    @Throws(ClassNotFoundException::class)
    fun getClass(ctClass: CtClass, loader: ClassLoader?): Class<*> {
        return if (classMap.containsKey(ctClass.name)) {
            classMap[ctClass.name]!!
        } else {
            //Class<?> clz = ctClass.toClass(loader, null);  // throw duplicate error, no reuse
            val clz = Class.forName(ctClass.name, false, loader)
            classMap[ctClass.name] = clz
            clz
        }
    }

    fun clear() {
        pathMap.clear()
        classMap.clear()
        callAliasMap.clear()
    }

    fun insertCallAlias(callAlias: String, cc: CtClass): String? {
        if (TextUtil.isEmpty(callAlias)) {
            return null
        }
        if (!callAliasMap.containsKey(callAlias)) {
            callAliasMap[callAlias] = cc.name
            return null
        }
        return callAliasMap[callAlias]
    }
}
