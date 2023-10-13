package com.didi.drouter.generator

import com.didi.drouter.plugin.RouterSetting.Parse
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.StoreUtil
import com.didi.drouter.utils.TextUtil
import javassist.ClassPool
import javassist.CtClass
import javassist.CtNewMethod
import javassist.Loader
import javassist.NotFoundException
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.annotation.Annotation
import java.io.File
import java.lang.reflect.Modifier

/**
 * Created by gaowei on 2018/8/30
 */
abstract class AbsRouterCollect(var pool: ClassPool, var setting: Parse) {
    abstract fun collect(ct: CtClass): Boolean
    @Throws(Exception::class)
    abstract fun generate(routerDir: File)
    abstract fun include(superCt: CtClass): Boolean
    var classLoader: Loader = Loader(pool)

    val packageName: String
        get() = if (TextUtil.isEmpty(setting.pluginName)) {
            "com.didi.drouter.loader.host"
        } else {
            "com.didi.drouter.loader.${setting.pluginName}"
        }

    fun getAnnotation(ctClass: CtClass, annotation: Class<*>): Annotation? {
        if (ctClass.isFrozen) ctClass.defrost()
        val cf = ctClass.classFile
        val visibleAttr = cf.getAttribute(AnnotationsAttribute.visibleTag) as? AnnotationsAttribute
        val sp = visibleAttr?.getAnnotation(annotation.name)
        if (null != sp) return sp

        val invisibleAttr =
            cf.getAttribute(AnnotationsAttribute.invisibleTag) as AnnotationsAttribute
        return invisibleAttr.getAnnotation(annotation.name)
    }

    @Throws(Exception::class)
    fun generatorClass(routerDir: File, ctClass: CtClass, vararg methods: String?) {
        for (method in methods) {
            val ctMethod = CtNewMethod.make(method, ctClass)
            val methodInfo = ctMethod.methodInfo
            val constPool = ctClass.classFile.constPool
            val methodAttr = AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag)
            val overRide = Annotation("java.lang.Override", constPool)
            methodAttr.addAnnotation(overRide)
            methodInfo.addAttribute(methodAttr)
            ctClass.addMethod(ctMethod)
        }
        ctClass.writeFile(routerDir.canonicalPath)
    }

    @Throws(ClassNotFoundException::class)
    fun isNonStaticInnerClass(ctClass: CtClass): Boolean {
        val possible = ctClass.name.contains("$")
        if (possible) {
            val clz = StoreUtil.getClass(ctClass, classLoader)
            return clz.modifiers and Modifier.STATIC == 0
        }
        return false
    }

    // support static inner class, used for annotation "..A.B" not "..A$B"
    @Throws(NotFoundException::class)
    fun getCtClass(className: String): CtClass {
        var tempClassName = className
        var ctClass: CtClass
        var exception: NotFoundException? = null
        while (true) {
            try {
                ctClass = pool[tempClassName]
                break
            } catch (e: NotFoundException) {
                if (exception == null) exception = e
                val index = tempClassName.lastIndexOf(".")
                tempClassName = if (index != -1) {
                    tempClassName.substring(0, index) + "$" + tempClassName.substring(index + 1)
                } else {
                    throw exception
                }
            }
        }
        return ctClass
    }

    // include self
    fun collectSuper(ct: CtClass?): Set<CtClass> {
        var tempCt = ct
        val collect: MutableSet<CtClass> = HashSet()
        try {
            while (tempCt != null) {
                collect.add(tempCt)
                collectInterface(tempCt, collect)
                tempCt = tempCt.superclass
            }
        } catch (e: NotFoundException) {
            // ignore
        }
        return collect
    }

    private fun collectInterface(ct: CtClass?, collect: MutableSet<CtClass>) {
        try {
            if (ct != null) {
                for (superInterface in ct.interfaces) {
                    collect.add(superInterface)
                    collectInterface(superInterface, collect)
                }
            }
        } catch (e: NotFoundException) {
            // ignore
        }
    }

    // check all super class and interface, include self
    // As long as any of the super ct contains classNames return yes.
    fun checkSuper(ct: CtClass?, vararg classNames: String): Boolean {
        var tempCtClass = ct
        try {
            while (tempCtClass != null) {
                if (match(tempCtClass, *classNames)) {   //self
                    return true
                }
                if (checkInterface(tempCtClass, *classNames)) {
                    return true
                }
                tempCtClass = tempCtClass.superclass
            }
        } catch (e: NotFoundException) {
            return classNames.contains(e.message)
        }
        return false
    }

    // ct can be class or interface, include self, tree
    private fun checkInterface(ct: CtClass?, vararg classNames: String): Boolean {
        if (ct == null) {
            return false
        }
        if (match(ct, *classNames)) {
            return true
        }
        try {
            for (superInterface in ct.interfaces) {
                val r = checkInterface(superInterface, *classNames)
                if (r) {
                    return true
                }
            }
        } catch (e: NotFoundException) {
            // ignore
        }
        return false
    }

    private fun match(ct: CtClass, vararg classNames: String): Boolean {
        for (name in classNames) {
            if (name == ct.name) {
                return true
            }
        }
        return false
    }

    companion object {
        const val MATCH = "com.didi.drouter.match."
        const val PROXY = "com.didi.drouter.proxy."
        const val METHOD1 =
            "public java.lang.Object newInstance(android.content.Context context) {" +
                    "   return null;" +
                    "}"
        const val METHOD2 =
            "public java.lang.Object callMethod(Object instance, String methodName, Object[] args) {" +
                    "   return null;" +
                    "}"
    }
}
