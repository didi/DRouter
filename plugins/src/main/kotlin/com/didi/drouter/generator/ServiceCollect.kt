package com.didi.drouter.generator

import com.didi.drouter.annotation.Assign
import com.didi.drouter.annotation.Remote
import com.didi.drouter.annotation.Service
import com.didi.drouter.plugin.RouterSetting.Parse
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.StoreUtil
import com.didi.drouter.utils.TextUtil
import javassist.ClassPool
import javassist.CtClass
import javassist.NotFoundException
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.ClassMemberValue
import java.io.File
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by gaowei on 2018/8/30
 */
class ServiceCollect internal constructor(pool: ClassPool, setting: Parse) :
    AbsRouterCollect(pool, setting) {
    private val serviceClass: MutableMap<String, CtClass> = ConcurrentHashMap()
    private val items: MutableList<String> = ArrayList()

    override fun collect(ct: CtClass): Boolean {
        if (include(ct)) {
            serviceClass[ct.name] = ct
            return true
        }
        return false
    }

    @Throws(Exception::class)
    override fun generate(routerDir: File) {
        val ctClass = pool.makeClass("$packageName.ServiceLoader")
        val superClass = pool["com.didi.drouter.store.MetaLoader"]
        ctClass.superclass = superClass
        val builder = StringBuilder()
        builder.append("public void load(java.util.Map data) {\n")
        val featureInterface = pool["com.didi.drouter.service.IFeatureMatcher"]
        for (serviceCc in serviceClass.values) {
            try {
                if (isNonStaticInnerClass(serviceCc)) {
                    throw Exception("Annotation can not use non static inner class")
                }
                val aliasValue = (serviceCc.getAnnotation(Service::class.java) as? Service)?.alias
                val priority = (serviceCc.getAnnotation(Service::class.java) as? Service)?.priority
                val cacheValue = (serviceCc.getAnnotation(Service::class.java) as? Service)?.cache

                val annotation = getAnnotation(serviceCc, Service::class.java)
                val featureValue = (annotation?.getMemberValue("feature") as? ArrayMemberValue)
                val functionCcList: MutableList<CtClass?> = ArrayList()
                val anyAbility = handleFunctionValue(annotation, functionCcList, aliasValue, serviceCc)
                if (anyAbility) {
                    functionCcList.clear()
                    functionCcList.addAll(collectSuper(serviceCc))
                    if ((aliasValue?.size ?: 0) > 1) {
                        throw Exception("only use one alias at most to match AnyAbility")
                    }
                    if ((featureValue?.value?.size ?: 0) > 1) {
                        throw Exception("only use one feature at most to match AnyAbility")
                    }
                }

                traversalArgs(
                    functionCcList,
                    aliasValue,
                    featureValue,
                    serviceCc,
                    featureInterface,
                    routerDir,
                    priority,
                    cacheValue
                )
            } catch (e: Exception) {
                throw Exception(
                    """Class: === ${serviceCc.name} ===
Cause: ${e.message}""", e
                )
            }
        }
        items.sort()
        for (item in items) {
            builder.append(item)
        }
        builder.append("}")
        Logger.d("\nclass ServiceLoader\n$builder")
        generatorClass(routerDir, ctClass, builder.toString())
    }

    /**
     * traverse all the function argument, one function corresponding one function->(impl,feature) data
     */
    private fun traversalArgs(
        functionCcList: MutableList<CtClass?>,
        aliasValue: Array<String>?,
        featureValue: ArrayMemberValue?,
        serviceCc: CtClass,
        featureInterface: CtClass?,
        routerDir: File,
        priority: Int?,
        cacheValue: Int?
    ) {
        for (i in functionCcList.indices) {
            val functionCc = functionCcList[i]
            val alias = when {
                aliasValue == null -> ""
                (aliasValue.size == 1) -> aliasValue[0]
                i < aliasValue.size -> aliasValue[i]  // affirm no AnyAbility
                else -> ""
            }

            // one feature generator one feature matcher class
            var featureCc: CtClass? = null
            var featureMatchCc: CtClass? = null
            if (featureValue != null) {
                if (featureValue.value.size == 1) {
                    val featureCmv = featureValue.value[0] as ClassMemberValue
                    featureCc = pool[featureCmv.value]
                } else if (i < featureValue.value.size) {   // affirm no AnyAbility
                    val featureCmv = featureValue.value[i] as ClassMemberValue
                    featureCc = pool[featureCmv.value]
                }
            }
            if (featureCc != null) {
                // avoid class duplication
                val featureMatcher: String =
                    MATCH + serviceCc.name.replace(
                        ".",
                        "_"
                    ) + "__" + featureCc.simpleName
                featureMatchCc = pool.getOrNull(featureMatcher)
                if (featureMatchCc == null) {
                    featureMatchCc = pool.makeClass(featureMatcher)
                    featureMatchCc.addInterface(featureInterface)
                    val featureBuilder = StringBuilder()
                    featureBuilder.append("\npublic boolean match(Object obj) {")
                    featureBuilder.append("\n    return obj instanceof ")
                    featureBuilder.append(featureCc.name)
                    completeMatchMethod(serviceCc, featureCc, featureBuilder)
                    featureBuilder.append(";\n}")
                    //                            Logger.d(featureBuilder.toString());
                    generatorClass(routerDir, featureMatchCc, featureBuilder.toString())
                }
            }
            var methodProxyCc: CtClass? = null
            var constructorMethod: String? = null
            var executeMethod: String? = null
            try {
                val constructor = serviceCc.getDeclaredConstructor(null)
                if (constructor != null) {
                    constructorMethod = String.format(
                        "public java.lang.Object newInstance(android.content.Context context) {" +
                                "{  return new %s();} }",
                        serviceCc.name
                    )
                }
            } catch (ignore: NotFoundException) {
            }
            val ctMethods = serviceCc.methods
            if (ctMethods != null) {
                val allIfStr = StringBuilder()
                val methodNames: MutableSet<String> = HashSet()
                for (method in ctMethods) {
                    val add =
                        methodNames.add(method.name + "_$\$_" + method.parameterTypes.size)
                    val remote = method.getAnnotation(Remote::class.java) as? Remote
                    if (remote != null) {
                        if (!add) {
                            throw Exception(
                                String.format(
                                    "The method \"%s\" with @Remote " +
                                            "can't be same name and same parameter count",
                                    method.name
                                )
                            )
                        }
                        val returnCc = method.returnType
                        checkPrimitiveType(method.name, returnCc)
                        val paraTypeCts = method.parameterTypes
                        val para = StringBuilder()
                        if (paraTypeCts != null) {
                            // argument type
                            for (j in paraTypeCts.indices) {
                                checkPrimitiveType(method.name, paraTypeCts[j])
                                para.append(
                                    String.format(
                                        "(%s) (args[%s])",
                                        paraTypeCts[j].name,
                                        j
                                    )
                                )
                                if (j != paraTypeCts.size - 1) {
                                    para.append(",")
                                }
                            }
                        }
                        // return type
                        if ("void" != returnCc.name) {
                            allIfStr.append(
                                String.format(
                                    "if (\"%s\".equals(methodName)) { return ((%s)instance).%s(%s); }",
                                    method.name + "_$\$_" + method.parameterTypes.size,
                                    serviceCc.name, method.name, para
                                )
                            )
                        } else {
                            allIfStr.append(
                                String.format(
                                    "if (\"%s\".equals(methodName)) { ((%s)instance).%s(%s); return null; }",
                                    method.name + "_$\$_" + method.parameterTypes.size,
                                    serviceCc.name, method.name, para
                                )
                            )
                        }
                    }
                }
                executeMethod = String.format(
                    "public java.lang.Object callMethod(Object instance, String methodName, Object[] " +
                            "args) {" +
                            "%s" +
                            "throw " +
                            "new com.didi.drouter.store.IRouterProxy.RemoteMethodMatchException();" +
                            "}",
                    allIfStr
                )
            }
            if (constructorMethod != null || executeMethod != null) {
                val proxyInterface = pool["com.didi.drouter.store.IRouterProxy"]
                val path: String =
                    PROXY + serviceCc.name.replace(".", "_")
                methodProxyCc = pool.getOrNull(path)
                if (methodProxyCc == null) {
                    methodProxyCc = pool.makeClass(path)
                    methodProxyCc.addInterface(proxyInterface)
                    generatorClass(
                        routerDir, methodProxyCc,
                        constructorMethod ?: METHOD1,
                        executeMethod ?: METHOD2
                    )
                }
            }
            val itemBuilder = StringBuilder()
            itemBuilder.append("    put(")
            itemBuilder.append(functionCc!!.name)
            itemBuilder.append(".class, com.didi.drouter.store.RouterMeta.build(")
            itemBuilder.append("com.didi.drouter.store.RouterMeta.SERVICE)")
            itemBuilder.append(".assembleService(")
            itemBuilder.append(serviceCc.name)
            itemBuilder.append(".class, ")
            itemBuilder.append(if (methodProxyCc != null) "new " + methodProxyCc.name + "()" else "null")
            itemBuilder.append(", \"")
            itemBuilder.append(alias)
            itemBuilder.append("\",")
            itemBuilder.append(if (featureMatchCc != null) "new " + featureMatchCc.name + "()" else "null")
            itemBuilder.append(",")
            itemBuilder.append(priority)
            itemBuilder.append(",")
            itemBuilder.append(cacheValue)
            itemBuilder.append(")")
            itemBuilder.append(", data);\n")
            items.add(itemBuilder.toString())
        }
    }

    private fun handleFunctionValue(
        annotation: Annotation?,
        functionCcList: MutableList<CtClass?>,
        aliasValue: Array<String>?,
        serviceCc: CtClass
    ): Boolean {
        var anyAbility = false
        val functionValue = (annotation?.getMemberValue("function") as? ArrayMemberValue)?: return false
        for (i in functionValue.value.indices) {
            val functionCmv = functionValue.value[i] as ClassMemberValue
            if ("com.didi.drouter.service.AnyAbility" == functionCmv.value) {
                anyAbility = true
            } else {
                val functionCc = getCtClass(functionCmv.value)
                functionCcList.add(functionCc)
                var superClassNames: Array<String>
                if ("com.didi.drouter.service.ICallService" == functionCmv.value) {
                    superClassNames = Array(7) { "" }
                    for (j in 0..5) {
                        superClassNames[j] = "com.didi.drouter.service.ICallService\$Type$j"
                    }
                    superClassNames[6] = "com.didi.drouter.service.ICallService\$TypeN"
                } else {
                    superClassNames = arrayOf(functionCc.name)
                }
                // ICallService should use unique alias, for using alias to determine which service
                if (aliasValue != null && functionCmv.value.startsWith("com.didi.drouter.service.ICallService")) {
                    if (i <= aliasValue.size - 1) {
                        val duplicate = StoreUtil.insertCallAlias(aliasValue[i], serviceCc)
                        if (duplicate != null) {
                            throw Exception("ICallService can't use the same alias with$duplicate")
                        }
                    }
                }
                if (!checkSuper(serviceCc, *superClassNames)) {
                    throw Exception("@Service with function does not match interface")
                }
            }
        }
        return anyAbility
    }

    @Throws(Exception::class)
    private fun completeMatchMethod(
        serviceCc: CtClass, featureCc: CtClass,
        featureBuilder: StringBuilder
    ) {
        val serviceFields = serviceCc.declaredFields
        val featureFields = featureCc.declaredFields
        for (serviceField in serviceFields) {
            if (serviceField.hasAnnotation(Assign::class.java)) {
                val fieldTypeCc = serviceField.type //the type of property: int,int[],
                //Logger.v("type: " + fieldTypeCc.getName());

                // remain basic,string,their array
                if (fieldTypeCc.name.contains(".") &&
                    "java.lang.String" != fieldTypeCc.name &&
                    "java.lang.String[]" != fieldTypeCc.name ||
                    fieldTypeCc.name.contains("[][]")
                ) {
                    throw Exception(
                        "@Assign should use correct type: " +
                                serviceCc.simpleName + "." + serviceField.name
                    )
                }
                if (!(Modifier.isStatic(serviceField.modifiers) &&
                            Modifier.isPublic(serviceField.modifiers) &&
                            Modifier.isFinal(serviceField.modifiers))
                ) {
                    throw Exception(
                        "@Assign should use \"public static final\" type: " +
                                serviceCc.simpleName + "." + serviceField.name
                    )
                }
                val property = serviceField.getAnnotation(Assign::class.java) as Assign
                val propertyName =
                    if (TextUtil.isEmpty(property.name)) serviceField.name else property.name
                val propertyValue: Any? = if (fieldTypeCc.name.contains("[]")) {
                    val cls = StoreUtil.getClass(serviceCc, classLoader)
                    val field = cls.getField(serviceField.name)
                    field[null]
                } else {
                    serviceField.constantValue //char is regard as int
                }
                var isHasMatch = false
                for (j in featureFields.indices) {
                    // check property name match first, and then its type must be match
                    if (featureFields[j] != null && featureFields[j]!!.name == propertyName) {
                        if (serviceField.type.name != featureFields[j]!!.type.name &&
                            serviceField.type.name != featureFields[j]!!.type.name + "[]"
                        ) {
                            throw Exception(
                                "@Assign field type should be matched with feature field type: " +
                                        featureCc.simpleName + "." + featureFields[j]!!.name
                            )
                        }
                        featureFields[j] = null
                        isHasMatch = true
                    }
                }
                // support multi feature, so Assign may be redundant, then ignore it.
                if (!isHasMatch) {
                    continue
                }

                // each Assign generator one && sentence
                featureBuilder.append(" &&\n      ")
                if (fieldTypeCc.name.startsWith("java.lang.String")) {
                    // string or string array
                    featureBuilder.append("(")
                    if (propertyValue is Array<*> && propertyValue.isArrayOf<String>()) {
                        for (value in propertyValue) {
                            appendFeatureString(featureBuilder, featureCc.name, propertyName, value)
                        }
                    } else {
                        appendFeatureString(
                            featureBuilder,
                            featureCc.name,
                            propertyName,
                            propertyValue
                        )
                    }
                    if (featureBuilder.toString().endsWith(" || ")) {
                        featureBuilder.delete(featureBuilder.length - 4, featureBuilder.length)
                    }
                    featureBuilder.append(")")
                } else {
                    // basic or basic array
                    featureBuilder.append("(")
                    when (propertyValue) {
                        is IntArray -> {
                            for (value in propertyValue) {
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        is ShortArray -> {
                            for (value in propertyValue) {
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        is LongArray -> {
                            for (value in propertyValue) {
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        is ByteArray -> {
                            for (value in propertyValue) {
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        is CharArray -> {
                            for (value in propertyValue) {
                                // must has single quotes as '1', then can be get its value
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        is FloatArray -> {
                            for (value in propertyValue) {
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        is DoubleArray -> {
                            for (value in propertyValue) {
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        is BooleanArray -> {
                            for (value in propertyValue) {
                                appendFeatureBasic(featureBuilder, featureCc.name, propertyName, value)
                            }
                        }

                        else -> {
                            appendFeatureBasic(
                                featureBuilder,
                                featureCc.name,
                                propertyName,
                                propertyValue
                            )
                        }
                    }
                    if (featureBuilder.toString().endsWith(" || ")) {
                        featureBuilder.delete(featureBuilder.length - 4, featureBuilder.length)
                    }
                    featureBuilder.append(")")
                }
            }
        }
        // check the rest of property, whether has no matcher.
        for (beanField in featureFields) {
            if (beanField != null) {
                throw Exception(
                    "should use @Assign to match this field: " +
                            featureCc.simpleName + "." + beanField.name
                )
            }
        }
    }

    override fun include(superCt: CtClass): Boolean {
        return superCt.hasAnnotation(Service::class.java)
    }

    private fun appendFeatureBasic(
        featureBuilder: StringBuilder, featureClass: String,
        propertyName: String, value: Any?
    ) {
        featureBuilder.append("((")
        featureBuilder.append(featureClass)
        featureBuilder.append(")obj).")
        featureBuilder.append(propertyName)
        featureBuilder.append(" == ")
        featureBuilder.append(if (value is Char) "'$value'" else value)
        featureBuilder.append(" || ")
    }

    private fun appendFeatureString(
        featureBuilder: StringBuilder, featureClass: String,
        propertyName: String, value: Any?
    ) {
        featureBuilder.append("android.text.TextUtils.equals(")
        featureBuilder.append("((")
        featureBuilder.append(featureClass)
        featureBuilder.append(")obj).")
        featureBuilder.append(propertyName)
        featureBuilder.append(", ")
        featureBuilder.append(if (value == null) "" else "\"")
        featureBuilder.append(value) //null->"null"
        featureBuilder.append(if (value == null) "" else "\"")
        featureBuilder.append(")")
        featureBuilder.append(" || ")
    }

    @Throws(Exception::class)
    private fun checkPrimitiveType(method: String, clz: CtClass) {
        val check =
            "byte" == clz.name || "short" == clz.name || "int" == clz.name || "long" == clz.name || "float" == clz.name || "double" == clz.name || "char" == clz.name || "boolean" == clz.name
        if (check) {
            throw Exception(
                String.format(
                    "The type \"%s\" in method \"%s\" with @Remote " +
                            "can't use primitive type",
                    clz.name, method
                )
            )
        }
    }
}
