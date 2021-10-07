package com.didi.drouter.generator;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.plugin.RouterProperties;
import com.didi.drouter.plugin.RouterSetting;
import com.didi.drouter.utils.Logger;
import com.didi.drouter.utils.StoreUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

/**
 * Created by gaowei on 2018/8/30
 */
class RouterCollect extends AbsRouterCollect {

    private final Map<String, CtClass> routerClass = new ConcurrentHashMap<>();
    private final Pattern pattern = Pattern.compile("[\\w/]*");  // \w or /
    // <> inside can't contains < or >, this pattern means using placeholder
    private final Pattern placeholderPattern = Pattern.compile("<[^<>]*>");
    private final List<String> items = new ArrayList<>();

    RouterCollect(ClassPool pool, RouterSetting setting) {
        super(pool, setting);
    }

    @Override
    public boolean collect(CtClass ct) {
        if (include(ct)) {
            routerClass.put(ct.getName(), ct);
            return true;
        }
        return false;
    }

    @Override
    public void generate(File routerDir) throws Exception {
        CtClass ctClass = pool.makeClass(getPackageName() + ".RouterLoader");
        CtClass superClass = pool.get("com.didi.drouter.store.MetaLoader");
        ctClass.setSuperclass(superClass);

        StringBuilder builder = new StringBuilder();
        builder.append("public void load(java.util.Map data) {\n");
        for (CtClass routerCc : routerClass.values()) {
            try {
                StringBuilder interceptorClass = null;
                StringBuilder interceptorName = null;

                String uriValue = "";
                String schemeValue = "";
                String hostValue = "";
                String pathValue = "";
                Annotation annotation = null;
                String type;
                int thread = 0;
                int priority = 0;
                boolean hold = false;
                if (routerCc.hasAnnotation(Router.class)) {
                    uriValue = ((Router) routerCc.getAnnotation(Router.class)).uri();
                    schemeValue = ((Router) routerCc.getAnnotation(Router.class)).scheme();
                    hostValue = ((Router) routerCc.getAnnotation(Router.class)).host();
                    pathValue = ((Router) routerCc.getAnnotation(Router.class)).path();
                    thread = ((Router) routerCc.getAnnotation(Router.class)).thread();
                    priority = ((Router) routerCc.getAnnotation(Router.class)).priority();
                    hold = ((Router) routerCc.getAnnotation(Router.class)).hold();
                    annotation = getAnnotation(routerCc, Router.class);
                    if (checkSuper(routerCc, "android.app.Activity")) {
                        type = "com.didi.drouter.store.RouterMeta.ACTIVITY";
                    } else if (checkSuper(routerCc,
                            "android.support.v4.app.Fragment", "androidx.fragment.app.Fragment")) {
                        type = "com.didi.drouter.store.RouterMeta.FRAGMENT";
                    } else if (checkSuper(routerCc, "android.view.View")) {
                        type = "com.didi.drouter.store.RouterMeta.VIEW";
                    } else if (checkSuper(routerCc, "com.didi.drouter.router.IRouterHandler")) {
                        type = "com.didi.drouter.store.RouterMeta.HANDLER";
                    } else {
                        throw new Exception("@Router target class illegal, " +
                                "support only Activity/Fragment/View/IRouterHandler");
                    }
                } else {
                    pathValue = "/" + routerCc.getName().replace(".", "/");
                    type = "com.didi.drouter.store.RouterMeta.ACTIVITY";
                }
                if (isNonStaticInnerClass(routerCc)) {
                    throw new Exception("@Router can not use non static inner class");
                }
                if (!uriValue.isEmpty()) {
                    if (!schemeValue.isEmpty() || !hostValue.isEmpty() || !pathValue.isEmpty()) {
                        throw new Exception("@Router uri can be used alone");
                    }
                    schemeValue = parseScheme(uriValue);
                    hostValue = parseHost(uriValue);
                    pathValue = parsePath(uriValue);
                }
                if (schemeValue.contains("/") || hostValue.contains("/")) {
                    throw new Exception("@Router scheme and host can't use \"/\"");
                }
//                if (!isRegex(pathValue) && !pathValue.isEmpty() && !pathValue.startsWith("/")) {
//                    throw new Exception("@Router path must start with \"/\"");
//                }

                // because of Escape character, \ will drop one, \\\\->\\, \\->empty(can't be one)
                schemeValue = schemeValue.replace("\\", "\\\\");
                hostValue = hostValue.replace("\\", "\\\\");
                pathValue = pathValue.replace("\\", "\\\\");

                if (annotation != null) {
                    ArrayMemberValue interceptorClassArrayValue =
                            (ArrayMemberValue) annotation.getMemberValue("interceptor");
                    if (interceptorClassArrayValue != null) {
                        interceptorClass = new StringBuilder();
                        interceptorClass.append("new Class[]{");
                        for (MemberValue mv : interceptorClassArrayValue.getValue()) {
                            final ClassMemberValue cmv = (ClassMemberValue) mv;
                            interceptorClass.append(cmv.getValue());
                            interceptorClass.append(".class,");
                        }
                        interceptorClass.deleteCharAt(interceptorClass.length() - 1);
                        interceptorClass.append("}");
                    }

                    ArrayMemberValue interceptorNameArrayValue =
                            (ArrayMemberValue) annotation.getMemberValue("interceptorName");
                    if (interceptorNameArrayValue != null) {
                        interceptorName = new StringBuilder();
                        interceptorName.append("new String[]{");
                        for (MemberValue mv : interceptorNameArrayValue.getValue()) {
                            final StringMemberValue smv = (StringMemberValue) mv;
                            interceptorName.append("\"");
                            interceptorName.append(smv.getValue());
                            interceptorName.append("\",");
                        }
                        interceptorName.deleteCharAt(interceptorName.length() - 1);
                        interceptorName.append("}");
                    }
                }

                StringBuilder metaBuilder = new StringBuilder();
                metaBuilder.append("com.didi.drouter.store.RouterMeta.build(");
                metaBuilder.append(type);
                metaBuilder.append(").assembleRouter(");
                metaBuilder.append("\"").append(schemeValue).append("\"");
                metaBuilder.append(",");
                metaBuilder.append("\"").append(hostValue).append("\"");
                metaBuilder.append(",");
                metaBuilder.append("\"").append(pathValue).append("\"");
                metaBuilder.append(",");
                if ("com.didi.drouter.store.RouterMeta.ACTIVITY".equals(type)) {
                    // !setting.getUseActivityRouterClass()
                    if (!RouterProperties.useActivityRouterClass) {
                        metaBuilder.append("\"").append(routerCc.getName()).append("\"");
                    } else {
                        metaBuilder.append(routerCc.getName()).append(".class");
                    }
                } else {
                    metaBuilder.append(routerCc.getName()).append(".class");
                }
                metaBuilder.append(", ");
                CtClass proxyCc = null;
                try {
                    if (type.endsWith("HANDLER") || type.endsWith("FRAGMENT")) {
                        CtConstructor constructor = routerCc.getDeclaredConstructor(null);
                        if (constructor != null) {
                            CtClass proxyInterface = pool.get("com.didi.drouter.store.IRouterProxy");
                            proxyCc = pool.makeClass(PROXY +
                                    routerCc.getName().replace(".", "_"));
                            proxyCc.addInterface(proxyInterface);
                            String method1 = String.format(
                                    "public java.lang.Object newInstance(android.content.Context context) {" +
                                            "{  return new %s();} }",
                                    routerCc.getName());
                            generatorClass(routerDir, proxyCc, method1, METHOD2);
                        }
                    } else if (type.endsWith("VIEW")) {
                        CtConstructor constructor =
                                routerCc.getDeclaredConstructor(new CtClass[] {pool.get("android.content.Context")});
                        if (constructor != null) {
                            CtClass proxyInterface = pool.get("com.didi.drouter.store.IRouterProxy");
                            proxyCc = pool.makeClass(PROXY +
                                    routerCc.getName().replace(".", "_"));
                            proxyCc.addInterface(proxyInterface);
                            String method1 = String.format(
                                    "public java.lang.Object newInstance(android.content.Context context) {" +
                                            "{  return new %s(context);} }",
                                    routerCc.getName());
                            generatorClass(routerDir, proxyCc, method1, METHOD2);
                        }
                    }
                } catch (NotFoundException ignore) {
                }
                metaBuilder.append(proxyCc != null ? "new " + proxyCc.getName() + "()" : "null");
                metaBuilder.append(", ");
                metaBuilder.append(interceptorClass != null ? interceptorClass.toString() : "null");
                metaBuilder.append(", ");
                metaBuilder.append(interceptorName != null ? interceptorName.toString() : "null");
                metaBuilder.append(", ");
                metaBuilder.append(thread);
                metaBuilder.append(", ");
                metaBuilder.append(priority);
                metaBuilder.append(", ");
                metaBuilder.append(hold);
                metaBuilder.append(")");

                String uri = schemeValue + "@@" + hostValue + "$$" + pathValue;
                if (!isPlaceholderLegal(schemeValue, hostValue, pathValue)) {
                    throw new Exception("\"" + uri + "\" on " + routerCc.getName() +
                            "\ncan't use regex outside placeholder <>," +
                            "\nand must be unique legal identifier inside placeholder <>");
                }
                boolean isAnyRegex = isRegex(schemeValue, hostValue, pathValue);
                if (isAnyRegex) {
                    items.add("    put(\"" + uri + "\", " + metaBuilder + ", data); \n");
                    //builder.append("    put(\"").append(uri).append("\", ").append(metaBuilder).append(", data); \n");
                } else {
                    items.add("    data.put(\"" + uri + "\", " + metaBuilder + "); \n");
                    //builder.append("    data.put(\"").append(uri).append("\", ").append(metaBuilder).append("); \n");
                }
                String duplicate = StoreUtil.insertUri(uri, routerCc);
                if (duplicate != null) {
                    throw new Exception("\"" + uri + "\" on " + routerCc.getName() +
                            "\nhas duplication of name with class: " + duplicate);
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Class: === " + routerCc.getName() + " ===\nCause: " + e.getMessage());
            }
        }
        Collections.sort(items);
        for (String item : items) {
            builder.append(item);
        }
        builder.append("}");

        Logger.d("\nclass RouterLoader" + "\n" + builder.toString());
        generatorClass(routerDir, ctClass, builder.toString());
    }

    @Override
    public boolean include(CtClass ct) {
        boolean r = ct.hasAnnotation(Router.class);
        if (setting.isSupportNoAnnotationActivity()) {
            return r || checkSuper(ct, "android.app.Activity");
        }
        return r;
    }

    private boolean isRegex(String... strings) {
        for (String string : strings) {
            if (!pattern.matcher(string).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlaceholderLegal(String... strings) {
        Set<String> identifier = new HashSet<>();
        for (String string : strings) {
            Matcher matcher = placeholderPattern.matcher(string);
            boolean isMatcher = false;
            // inside <>, must unique and identifier
            while (matcher.find()) {
                isMatcher = true;
                String placeholder = matcher.group();
                if (!placeholder.matches("<[a-zA-Z_]+\\w*>")) {
                    return false;
                }
                // unique
                if (!identifier.add(placeholder)) {
                    return false;
                }
            }
            // outside <>, must be \w or /
            if (isMatcher) {
                String[] splits = placeholderPattern.split(string);
                for (String split : splits) {
                    if (!pattern.matcher(split).matches()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static String parseScheme(String uriString) {
        int index = uriString.indexOf("://");
        if (index != -1) {
            return uriString.substring(0, index);
        } else {
            return "";
        }
    }

    private static String parseHost(String uriString) {
        int index = uriString.indexOf("://");
        if (index != -1) {
            uriString = uriString.substring(index + 3);
            index = uriString.indexOf("/");
            if (index != -1) {
                uriString = uriString.substring(0, index);
            }
            return uriString;
        } else {
            return "";
        }
    }

    private static String parsePath(String uriString) {
        int index = uriString.indexOf("://");
        if (index != -1) {
            uriString = uriString.substring(index + 3);
            index = uriString.indexOf("/");
            if (index != -1) {
                return uriString.substring(index);
            } else {
                return "";
            }
        } else {
            return uriString;
        }
    }
}
