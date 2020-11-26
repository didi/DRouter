package com.didi.drouter.generator;

import com.didi.drouter.plugin.RouterSetting;
import com.didi.drouter.utils.StoreUtil;
import com.didi.drouter.utils.TextUtil;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Loader;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;

/**
 * Created by gaowei on 2018/8/30
 */
abstract class AbsRouterCollect {

    static final String MATCH = "com.didi.drouter.match.";
    static final String PROXY = "com.didi.drouter.proxy.";
    static final String METHOD1 =
            "public java.lang.Object newInstance(android.content.Context context) {" +
            "   return null;" +
            "}";
    static final String METHOD2 =
            "public java.lang.Object execute(Object instance, String methodName, Object[] args) {" +
            "   return null;" +
            "}";

    abstract boolean collect(CtClass ct);
    abstract void generate(File routerDir) throws Exception;
    abstract boolean include(CtClass superCt);

    ClassPool pool;
    RouterSetting setting;
    Loader classLoader;

    AbsRouterCollect(ClassPool pool, RouterSetting setting) {
        this.pool = pool;
        this.setting = setting;
        this.classLoader = new Loader(pool);
    }

    String getPackageName() {
        if (TextUtil.isEmpty(setting.getPluginName())) {
            return "com.didi.drouter.loader.host";
        } else {
            return "com.didi.drouter.loader." + setting.getPluginName();
        }
    }

    Annotation getAnnotation(final CtClass ctClass, Class<?> annotation) {
        if (ctClass.isFrozen()) ctClass.defrost();

        ClassFile cf = ctClass.getClassFile();
        final AnnotationsAttribute visibleAttr =
                (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.visibleTag);
        if (null != visibleAttr) {
            final Annotation sp = visibleAttr.getAnnotation(annotation.getName());
            if (null != sp) {
                return sp;
            }
        }

        final AnnotationsAttribute invisibleAttr =
                (AnnotationsAttribute) cf.getAttribute(AnnotationsAttribute.invisibleTag);
        if (null != invisibleAttr) {
            final Annotation sp = invisibleAttr.getAnnotation(annotation.getName());
            if (null != sp) {
                return sp;
            }
        }
        return null;
    }

    void generatorClass(File routerDir, CtClass ctClass, String... methods) throws Exception {
        for (String method : methods) {
            CtMethod ctMethod = CtNewMethod.make(method, ctClass);

            MethodInfo methodInfo = ctMethod.getMethodInfo();
            ConstPool constPool = ctClass.getClassFile().getConstPool();
            AnnotationsAttribute methodAttr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            Annotation overRide = new Annotation("java.lang.Override", constPool);
            methodAttr.addAnnotation(overRide);
            methodInfo.addAttribute(methodAttr);

            ctClass.addMethod(ctMethod);
        }
        ctClass.writeFile(routerDir.getCanonicalPath());
    }

    boolean isNonStaticInnerClass(CtClass ctClass) throws ClassNotFoundException {
        boolean possible = ctClass.getName().contains("$");
        if (possible) {
            Class<?> clz = StoreUtil.getClass(ctClass, classLoader);
            return (clz.isLocalClass() || clz.isMemberClass()) && (clz.getModifiers() & Modifier.STATIC) == 0;
        }
        return false;
    }

    // support static inner class, used for annotation "..A.B" not "..A$B"
    CtClass getCtClass(String className) throws NotFoundException {
        CtClass ctClass;
        NotFoundException exception = null;
        while (true) {
            try {
                ctClass = pool.get(className);
                break;
            } catch (NotFoundException e) {
                if (exception == null) exception = e;
                int index = className.lastIndexOf(".");
                if (index != -1) {
                    className = className.substring(0, index) + "$" + className.substring(index + 1);
                } else {
                    throw exception;
                }
            }
        }
        return ctClass;
    }

    // include self
    Set<CtClass> collectSuper(CtClass ct) {
        Set<CtClass> collect = new HashSet<>();
        try {
            while (ct != null) {
                collect.add(ct);
                collectInterface(ct, collect);
                ct = ct.getSuperclass();
            }
        } catch (NotFoundException e) {
            // ignore
        }
        return collect;
    }

    void collectInterface(CtClass ct, Set<CtClass> collect) {
        try {
            if (ct != null) {
                for (CtClass superInterface : ct.getInterfaces()) {
                    collect.add(superInterface);
                    collectInterface(superInterface, collect);
                }
            }
        } catch (NotFoundException e) {
            // ignore
        }
    }

    // check all super class and interface, include self
    // As long as any of the super ct contains classNames return yes.
    boolean checkSuper(CtClass ct, String... classNames) {
        try {
            while (ct != null) {
                if (match(ct, classNames)) {   //self
                    return true;
                }
                if (checkInterface(ct, classNames)) {
                    return true;
                }
                ct = ct.getSuperclass();
            }
        } catch (NotFoundException e) {
            // ignore
        }
        return false;
    }

    // ct can be class or interface, include self, tree
    private boolean checkInterface(CtClass ct, String... classNames) {
        if (ct == null) {
            return false;
        }
        if (match(ct, classNames)) {
            return true;
        }
        try {
            for (CtClass superInterface : ct.getInterfaces()) {
                boolean r = checkInterface(superInterface, classNames);
                if (r) {
                    return true;
                }
            }
        } catch (NotFoundException e) {
            // ignore
        }
        return false;
    }

    private boolean match(CtClass ct, String... classNames) {
        for (String name : classNames) {
            if (name.equals(ct.getName())) {
                return true;
            }
        }
        return false;
    }
}
