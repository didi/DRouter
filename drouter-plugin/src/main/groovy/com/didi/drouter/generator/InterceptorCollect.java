package com.didi.drouter.generator;

import com.didi.drouter.annotation.Interceptor;
import com.didi.drouter.plugin.RouterSetting;
import com.didi.drouter.utils.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;

/**
 * Created by gaowei on 2018/8/30
 */
class InterceptorCollect extends AbsRouterCollect {

    private final Map<String, CtClass> interceptorClass = new ConcurrentHashMap<>();
    private final List<String> items = new ArrayList<>();

    InterceptorCollect(ClassPool pool, RouterSetting setting) {
        super(pool, setting);
    }

    @Override
    public boolean collect(CtClass ct) {
        if (include(ct)) {
            interceptorClass.put(ct.getName(), ct);
            return true;
        }
        return false;
    }

    @Override
    public void generate(File routerDir) throws Exception {
        CtClass ctClass = pool.makeClass(getPackageName() + ".InterceptorLoader");
        CtClass superClass = pool.get("com.didi.drouter.store.MetaLoader");
        ctClass.setSuperclass(superClass);

        StringBuilder builder = new StringBuilder();
        builder.append("\npublic void load(java.util.Map data) {\n");
        for (CtClass interceptorCc : interceptorClass.values()) {
            try {
                if (isNonStaticInnerClass(interceptorCc)) {
                    throw new Exception("Annotation can not use non static inner class");
                }
                if (!checkSuper(interceptorCc, "com.didi.drouter.router.IRouterInterceptor")) {
                    throw new Exception("@Interceptor class does not implement IRouterInterceptor interface");
                }

                Interceptor interceptor = (Interceptor) interceptorCc.getAnnotation(Interceptor.class);

                CtClass proxyCc = null;
                CtConstructor constructor = interceptorCc.getDeclaredConstructor(null);
                if (constructor != null) {
                    CtClass proxyInterface = pool.get("com.didi.drouter.store.IRouterProxy");
                    proxyCc = pool.makeClass(PROXY +
                            interceptorCc.getName().replace(".", "_"));
                    proxyCc.addInterface(proxyInterface);
                    String method1 = String.format(
                            "public java.lang.Object newInstance(android.content.Context context) {" +
                                    "{  return new %s();} }",
                            interceptorCc.getName());
                    generatorClass(routerDir, proxyCc, method1, METHOD2);
                }

                // class is the key
                StringBuilder itemBuilder = new StringBuilder();
                itemBuilder.append("    data.put(");
                itemBuilder.append(interceptorCc.getName());
                itemBuilder.append(".class");
                itemBuilder.append(", com.didi.drouter.store.RouterMeta.build(");
                itemBuilder.append("com.didi.drouter.store.RouterMeta.INTERCEPTOR)");
                itemBuilder.append(".assembleInterceptor(");
                itemBuilder.append(interceptorCc.getName());
                itemBuilder.append(".class, ");
                itemBuilder.append(proxyCc != null ? "new " + proxyCc.getName() + "()" : "null");
                itemBuilder.append(",");
                itemBuilder.append(interceptor.priority());
                itemBuilder.append(",");
                itemBuilder.append(interceptor.global());
                itemBuilder.append(",");
                itemBuilder.append(interceptor.cache());
                itemBuilder.append("));\n");
                items.add(itemBuilder.toString());
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Class: === " + interceptorCc.getName() + " ===\nCause: " + e.getMessage());
            }
        }
        Collections.sort(items);
        for (String item : items) {
            builder.append(item);
        }
        builder.append("}");

        Logger.d(builder.toString());
        generatorClass(routerDir, ctClass, builder.toString());
    }

    @Override
    public boolean include(CtClass ct) {
        return ct.hasAnnotation(Interceptor.class);
    }

}
