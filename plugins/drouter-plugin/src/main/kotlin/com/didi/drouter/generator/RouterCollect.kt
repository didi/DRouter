package com.didi.drouter.generator

import com.didi.drouter.annotation.Router
import com.didi.drouter.plugin.RouterSetting.Parse
import com.didi.drouter.utils.Logger
import com.didi.drouter.utils.StoreUtil
import javassist.ClassPool
import javassist.CtClass
import javassist.NotFoundException
import javassist.bytecode.annotation.Annotation
import javassist.bytecode.annotation.ArrayMemberValue
import javassist.bytecode.annotation.ClassMemberValue
import javassist.bytecode.annotation.StringMemberValue
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Created by gaowei on 2018/8/30
 */
internal class RouterCollect(pool: ClassPool, setting: Parse) : AbsRouterCollect(pool, setting) {
    private val routerClass: MutableMap<String, CtClass> = ConcurrentHashMap()
    private val pattern = Pattern.compile("[\\w/]*") // \w or /

    // <> inside can't contains < or >, this pattern means using placeholder
    private val placeholderPattern = Pattern.compile("<[^<>]*>")
    private val items: MutableList<String> = ArrayList()
    public override fun collect(ct: CtClass): Boolean {
        if (include(ct)) {
            routerClass[ct.name] = ct
            return true
        }
        return false
    }

    @Throws(Exception::class)
    override fun generate(routerDir: File) {
        val ctClass = pool.makeClass("$packageName.RouterLoader")
        val superClass = pool["com.didi.drouter.store.MetaLoader"]
        ctClass.superclass = superClass
        val builder = StringBuilder()
        builder.append("public void load(java.util.Map data) {\n")
        for (routerCc in routerClass.values) {
            try {
                var interceptorClass: StringBuilder? = null
                var interceptorName: StringBuilder? = null
                var uriValue = ""
                var schemeValue = ""
                var hostValue = ""
                var pathValue: String
                var annotation: Annotation? = null
                var type: String
                var thread = 0
                var priority = 0
                var hold = false
                if (routerCc.hasAnnotation(Router::class.java)) {
                    uriValue = (routerCc.getAnnotation(Router::class.java) as Router).uri
                    schemeValue = (routerCc.getAnnotation(Router::class.java) as Router).scheme
                    hostValue = (routerCc.getAnnotation(Router::class.java) as Router).host
                    pathValue = (routerCc.getAnnotation(Router::class.java) as Router).path
                    thread = (routerCc.getAnnotation(Router::class.java) as Router).thread
                    priority = (routerCc.getAnnotation(Router::class.java) as Router).priority
                    hold = (routerCc.getAnnotation(Router::class.java) as Router).hold
                    annotation = getAnnotation(routerCc, Router::class.java)
                    type = if (checkSuper(routerCc, "android.app.Activity")) {
                        "com.didi.drouter.store.RouterMeta.ACTIVITY"
                    } else if (checkSuper(
                            routerCc,
                            "android.support.v4.app.Fragment", "androidx.fragment.app.Fragment"
                        )
                    ) {
                        "com.didi.drouter.store.RouterMeta.FRAGMENT"
                    } else if (checkSuper(routerCc, "android.view.View")) {
                        "com.didi.drouter.store.RouterMeta.VIEW"
                    } else if (checkSuper(routerCc, "com.didi.drouter.router.IRouterHandler")) {
                        "com.didi.drouter.store.RouterMeta.HANDLER"
                    } else {
                        throw Exception(
                            "@Router target class illegal, " +
                                    "support only Activity/Fragment/View/IRouterHandler"
                        )
                    }
                } else {
                    pathValue = "/" + routerCc.name.replace(".", "/")
                    type = "com.didi.drouter.store.RouterMeta.ACTIVITY"
                }
                if (isNonStaticInnerClass(routerCc)) {
                    throw Exception("@Router can not use non static inner class")
                }
                if (uriValue.isNotEmpty()) {
                    if (schemeValue.isNotEmpty() || hostValue.isNotEmpty() || pathValue.isNotEmpty()) {
                        throw Exception("@Router uri can be used alone")
                    }
                    schemeValue = parseScheme(uriValue)
                    hostValue = parseHost(uriValue)
                    pathValue = parsePath(uriValue)
                }
                if (schemeValue.contains("/") || hostValue.contains("/")) {
                    throw Exception("@Router scheme and host can't use \"/\"")
                }
                //                if (!isRegex(pathValue) && !pathValue.isEmpty() && !pathValue.startsWith("/")) {
//                    throw new Exception("@Router path must start with \"/\"");
//                }

                // because of Escape character, \ will drop one, \\\\->\\, \\->empty(can't be one)
                schemeValue = schemeValue.replace("\\", "\\\\")
                hostValue = hostValue.replace("\\", "\\\\")
                pathValue = pathValue.replace("\\", "\\\\")
                if (annotation != null) {
                    val interceptorClassArrayValue =
                        annotation.getMemberValue("interceptor") as? ArrayMemberValue
                    if (interceptorClassArrayValue != null) {
                        interceptorClass = StringBuilder()
                        interceptorClass.append("new Class[]{")
                        for (mv in interceptorClassArrayValue.value) {
                            val cmv = mv as ClassMemberValue
                            interceptorClass.append(cmv.value)
                            interceptorClass.append(".class,")
                        }
                        interceptorClass.deleteCharAt(interceptorClass.length - 1)
                        interceptorClass.append("}")
                    }
                    val interceptorNameArrayValue =
                        annotation.getMemberValue("interceptorName") as? ArrayMemberValue
                    if (interceptorNameArrayValue != null) {
                        interceptorName = StringBuilder()
                        interceptorName.append("new String[]{")
                        for (mv in interceptorNameArrayValue.value) {
                            val smv = mv as StringMemberValue
                            interceptorName.append("\"")
                            interceptorName.append(smv.value)
                            interceptorName.append("\",")
                        }
                        interceptorName.deleteCharAt(interceptorName.length - 1)
                        interceptorName.append("}")
                    }
                }
                val metaBuilder = StringBuilder()
                metaBuilder.append("com.didi.drouter.store.RouterMeta.build(")
                metaBuilder.append(type)
                metaBuilder.append(").assembleRouter(")
                metaBuilder.append("\"").append(schemeValue).append("\"")
                metaBuilder.append(",")
                metaBuilder.append("\"").append(hostValue).append("\"")
                metaBuilder.append(",")
                metaBuilder.append("\"").append(pathValue).append("\"")
                metaBuilder.append(",")
                if ("com.didi.drouter.store.RouterMeta.ACTIVITY" == type) {
                    if (!setting.useActivityRouterClass) {
                        metaBuilder.append("\"").append(routerCc.name).append("\"")
                    } else {
                        metaBuilder.append(routerCc.name).append(".class")
                    }
                } else {
                    metaBuilder.append(routerCc.name).append(".class")
                }
                metaBuilder.append(", ")
                var proxyCc: CtClass? = null
                try {
                    if (type.endsWith("HANDLER") || type.endsWith("FRAGMENT")) {
                        val constructor = routerCc.getDeclaredConstructor(null)
                        if (constructor != null) {
                            val proxyInterface = pool["com.didi.drouter.store.IRouterProxy"]
                            proxyCc = pool.makeClass(
                                PROXY +
                                        routerCc.name.replace(".", "_")
                            )
                            proxyCc.addInterface(proxyInterface)
                            val method1 = String.format(
                                "public java.lang.Object newInstance(android.content.Context context) {" +
                                        "{  return new %s();} }",
                                routerCc.name
                            )
                            generatorClass(
                                routerDir,
                                proxyCc,
                                method1,
                                METHOD2
                            )
                        }
                    } else if (type.endsWith("VIEW")) {
                        val constructor = routerCc.getDeclaredConstructor(
                            arrayOf(
                                pool["android.content.Context"]
                            )
                        )
                        if (constructor != null) {
                            val proxyInterface = pool["com.didi.drouter.store.IRouterProxy"]
                            proxyCc = pool.makeClass(
                                AbsRouterCollect.Companion.PROXY +
                                        routerCc.name.replace(".", "_")
                            )
                            proxyCc.addInterface(proxyInterface)
                            val method1 = String.format(
                                "public java.lang.Object newInstance(android.content.Context context) {" +
                                        "{  return new %s(context);} }",
                                routerCc.name
                            )
                            generatorClass(
                                routerDir,
                                proxyCc,
                                method1,
                                AbsRouterCollect.Companion.METHOD2
                            )
                        }
                    }
                } catch (ignore: NotFoundException) {
                }
                metaBuilder.append(if (proxyCc != null) "new " + proxyCc.name + "()" else "null")
                metaBuilder.append(", ")
                metaBuilder.append(interceptorClass?.toString() ?: "null")
                metaBuilder.append(", ")
                metaBuilder.append(interceptorName?.toString() ?: "null")
                metaBuilder.append(", ")
                metaBuilder.append(thread)
                metaBuilder.append(", ")
                metaBuilder.append(priority)
                metaBuilder.append(", ")
                metaBuilder.append(hold)
                metaBuilder.append(")")
                val uri = "$schemeValue@@$hostValue$$$pathValue"
                if (!isPlaceholderLegal(schemeValue, hostValue, pathValue)) {
                    throw Exception(
                        """"$uri" on ${routerCc.name}
can't use regex outside placeholder <>,
and must be unique legal identifier inside placeholder <>"""
                    )
                }
                val isAnyRegex = isRegex(schemeValue, hostValue, pathValue)
                if (isAnyRegex) {
                    items.add("    put(\"$uri\", $metaBuilder, data); \n")
                    //builder.append("    put(\"").append(uri).append("\", ").append(metaBuilder).append(", data); \n");
                } else {
                    items.add("    data.put(\"$uri\", $metaBuilder); \n")
                    //builder.append("    data.put(\"").append(uri).append("\", ").append(metaBuilder).append("); \n");
                }
                val duplicate = StoreUtil.insertUri(uri, routerCc)
                if (duplicate != null) {
                    throw Exception(
                        """"$uri" on ${routerCc.name}
has duplication of name with class: $duplicate"""
                    )
                }
            } catch (e: Exception) {
                throw Exception(
                    """Class: === ${routerCc.name} ===
Cause: ${e.message}""", e
                )
            }
        }
        items.sort()
        for (item in items) {
            builder.append(item)
        }
        builder.append("}")
        Logger.d("\nclass RouterLoader\n$builder")
        generatorClass(routerDir, ctClass, builder.toString())
    }

    override fun include(superCt: CtClass): Boolean {
        val r = superCt.hasAnnotation(Router::class.java)
        return if (setting.supportNoAnnotationActivity) {
            r || checkSuper(superCt, "android.app.Activity")
        } else r
    }

    private fun isRegex(vararg strings: String): Boolean {
        for (string in strings) {
            if (!pattern.matcher(string).matches()) {
                return true
            }
        }
        return false
    }

    private fun isPlaceholderLegal(vararg strings: String): Boolean {
        val identifier: MutableSet<String> = HashSet()
        for (string in strings) {
            val matcher = placeholderPattern.matcher(string)
            var isMatcher = false
            // inside <>, must unique and identifier
            while (matcher.find()) {
                isMatcher = true
                val placeholder = matcher.group()
                if (!placeholder.matches("<[a-zA-Z_]+\\w*>".toRegex())) {
                    return false
                }
                // unique
                if (!identifier.add(placeholder)) {
                    return false
                }
            }
            // outside <>, must be \w or /
            if (isMatcher) {
                val splits = placeholderPattern.split(string)
                for (split in splits) {
                    if (!pattern.matcher(split).matches()) {
                        return false
                    }
                }
            }
        }
        return true
    }

    companion object {
        private fun parseScheme(uriString: String): String {
            val index = uriString.indexOf("://")
            return if (index != -1) {
                uriString.substring(0, index)
            } else {
                ""
            }
        }

        private fun parseHost(uriString: String): String {
            var tempUriString = uriString
            var index = tempUriString.indexOf("://")
            return if (index != -1) {
                tempUriString = tempUriString.substring(index + 3)
                index = tempUriString.indexOf("/")
                if (index != -1) {
                    tempUriString = tempUriString.substring(0, index)
                }
                tempUriString
            } else {
                ""
            }
        }

        private fun parsePath(uriString: String): String {
            var tempUriString = uriString
            var index = tempUriString.indexOf("://")
            return if (index != -1) {
                tempUriString = tempUriString.substring(index + 3)
                index = tempUriString.indexOf("/")
                if (index != -1) {
                    tempUriString.substring(index)
                } else {
                    ""
                }
            } else {
                tempUriString
            }
        }
    }
}
