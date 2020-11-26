package com.didi.drouter.utils;

import java.util.HashMap;
import java.util.Map;

import javassist.CtClass;

/**
 * Created by gaowei on 2018/10/30
 */
public class StoreUtil {

    private static Map<String, String> pathMap = new HashMap<>();
    private static Map<String, Class<?>> classMap = new HashMap<>();
    private static Map<String, String> callAliasMap = new HashMap<>();

    public static String insertUri(String uri, CtClass cc) {
        if (!pathMap.containsKey(uri)) {
            pathMap.put(uri, cc.getName());
            return null;
        }
        return pathMap.get(uri);
    }

    public static Class<?> getClass(CtClass ctClass, ClassLoader loader) throws ClassNotFoundException {
        if (classMap.containsKey(ctClass.getName())) {
            return classMap.get(ctClass.getName());
        } else {
            //Class<?> clz = ctClass.toClass(loader, null);  // throw duplicate error, no reuse
            Class<?> clz = Class.forName(ctClass.getName(), false, loader);
            ctClass.defrost();
            classMap.put(ctClass.getName(), clz);
            return clz;
        }
    }

    public static void clear() {
        pathMap.clear();
        classMap.clear();
        callAliasMap.clear();
    }

    public static String insertCallAlias(String callAlias, CtClass cc) {
        if (TextUtil.isEmpty(callAlias)) {
            return null;
        }
        if (!callAliasMap.containsKey(callAlias)) {
            callAliasMap.put(callAlias, cc.getName());
            return null;
        }
        return callAliasMap.get(callAlias);
    }
}
