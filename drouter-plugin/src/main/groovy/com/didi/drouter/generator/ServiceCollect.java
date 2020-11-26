package com.didi.drouter.generator;

import com.didi.drouter.annotation.Assign;
import com.didi.drouter.annotation.Remote;
import com.didi.drouter.annotation.Service;
import com.didi.drouter.plugin.RouterSetting;
import com.didi.drouter.utils.Logger;
import com.didi.drouter.utils.StoreUtil;
import com.didi.drouter.utils.TextUtil;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;

/**
 * Created by gaowei on 2018/8/30
 */
public class ServiceCollect extends AbsRouterCollect {

    private final Map<String, CtClass> serviceClass = new ConcurrentHashMap<>();
    private final List<String> items = new ArrayList<>();

    ServiceCollect(ClassPool pool, RouterSetting setting) {
        super(pool, setting);
    }

    @Override
    public boolean collect(CtClass ct) {
        if (include(ct)) {
            serviceClass.put(ct.getName(), ct);
            return true;
        }
        return false;
    }

    @Override
    public void generate(File routerDir) throws Exception {
        CtClass ctClass = pool.makeClass(getPackageName() + ".ServiceLoader");
        CtClass superClass = pool.get("com.didi.drouter.store.MetaLoader");
        ctClass.setSuperclass(superClass);

        StringBuilder builder = new StringBuilder();
        builder.append("\npublic void load(java.util.Map data) {\n");

        CtClass featureInterface = pool.get("com.didi.drouter.service.IFeatureMatcher");

        for (CtClass serviceCc : serviceClass.values()) {
            try {
                if (isNonStaticInnerClass(serviceCc)) {
                    throw new Exception("Annotation can not use non static inner class");
                }
                Annotation annotation = getAnnotation(serviceCc, Service.class);

                ArrayMemberValue functionValue = (ArrayMemberValue) annotation.getMemberValue("function");
                String[] aliasValue = ((Service)serviceCc.getAnnotation(Service.class)).alias();
                ArrayMemberValue featureValue = (ArrayMemberValue) annotation.getMemberValue("feature");
                int priority = ((Service)serviceCc.getAnnotation(Service.class)).priority();
                int cacheValue = ((Service)serviceCc.getAnnotation(Service.class)).cache();

                boolean anyAbility = false;
                List<CtClass> functionCcList = new ArrayList<>();
                for (int i = 0; i < functionValue.getValue().length; i++) {
                    ClassMemberValue functionCmv = (ClassMemberValue) functionValue.getValue()[i];
                    if ("com.didi.drouter.service.AnyAbility".equals(functionCmv.getValue())) {
                        anyAbility = true;
                    } else {
                        CtClass functionCc = getCtClass(functionCmv.getValue());
                        functionCcList.add(functionCc);
                        String[] superClassNames;
                        if ("com.didi.drouter.service.ICallService".equals(functionCmv.getValue())) {
                            superClassNames = new String[11];
                            for (int j = 0; j < 10; j++) {
                                superClassNames[j] = "com.didi.drouter.service.ICallService" + j;
                            }
                            superClassNames[10] = "com.didi.drouter.service.ICallServiceN";
                        } else {
                            superClassNames = new String[] {functionCc.getName()};
                        }
                        // ICallService should use unique alias, for using alias to determine which service
                        if (functionCmv.getValue().startsWith("com.didi.drouter.service.ICallService")) {
                            if (i <= aliasValue.length - 1) {
                                String duplicate = StoreUtil.insertCallAlias(aliasValue[i], serviceCc);
                                if (duplicate != null) {
                                    throw new Exception("ICallService can't use the same alias with" + duplicate);
                                }
                            }
                        }
                        if (!checkSuper(serviceCc, superClassNames)) {
                            throw new Exception("@Service with function does not match interface");
                        }
                    }
                }
                if (anyAbility) {
                    functionCcList.clear();
                    functionCcList.addAll(collectSuper(serviceCc));
                    if (aliasValue.length > 1) {
                        throw new Exception("only use one alias at most to match AnyAbility");
                    }
                    if (featureValue != null && featureValue.getValue().length > 1) {
                        throw new Exception("only use one feature at most to match AnyAbility");
                    }
                }

                // traverse all the function argument, one function corresponding one function->(impl,feature) data
                for (int i = 0; i < functionCcList.size(); i++) {
                    CtClass functionCc = functionCcList.get(i);

                    String alias = "";
                    if (aliasValue.length == 1) {
                        alias = aliasValue[0];
                    } else if (i < aliasValue.length) {   // affirm no AnyAbility
                        alias = aliasValue[i];
                    }

                    // one feature generator one feature matcher class
                    CtClass featureCc = null;
                    CtClass featureMatchCc = null;
                    if (featureValue != null) {
                        if (featureValue.getValue().length == 1) {
                            ClassMemberValue featureCmv = (ClassMemberValue) featureValue.getValue()[0];
                            featureCc = pool.get(featureCmv.getValue());
                        } else if (i < featureValue.getValue().length) {   // affirm no AnyAbility
                            ClassMemberValue featureCmv = (ClassMemberValue) featureValue.getValue()[i];
                            featureCc = pool.get(featureCmv.getValue());
                        }
                    }
                    if (featureCc != null) {
                        // avoid class duplication
                        String featureMatcher =
                                MATCH + serviceCc.getName().replace(".", "_") + "__" + featureCc.getSimpleName();
                        featureMatchCc = pool.getOrNull(featureMatcher);
                        if (featureMatchCc == null) {
                            featureMatchCc = pool.makeClass(featureMatcher);
                            featureMatchCc.addInterface(featureInterface);

                            StringBuilder featureBuilder = new StringBuilder();
                            featureBuilder.append("\npublic boolean match(Object obj) {");
                            featureBuilder.append("\n    return obj instanceof ");
                            featureBuilder.append(featureCc.getName());

                            completeMatchMethod(serviceCc, featureCc, featureBuilder);

                            featureBuilder.append(";\n}");
                            Logger.d(featureBuilder.toString());
                            generatorClass(routerDir, featureMatchCc, featureBuilder.toString());
                        }
                    }

                    CtClass proxyCc = null;
                    String method1 = null;
                    String method2 = null;
                    try {
                        CtConstructor constructor = serviceCc.getDeclaredConstructor(null);
                        if (constructor != null) {
                            method1 = String.format(
                                    "public java.lang.Object newInstance(android.content.Context context) {" +
                                    "{  return new %s();} }",
                                    serviceCc.getName());
                        }
                    } catch (NotFoundException ignore) {
                    }
                    CtMethod[] ctMethods = serviceCc.getMethods();
                    if (ctMethods != null) {
                        StringBuilder allIfStr = new StringBuilder();
                        Set<String> methodNames = new HashSet<>();
                        for (CtMethod method : ctMethods) {
                            boolean add =
                                    methodNames.add(method.getName() + "_$$_" + method.getParameterTypes().length);
                            Remote remote = (Remote) method.getAnnotation(Remote.class);
                            if (remote != null) {
                                if (!add) {
                                    throw new Exception(String.format("The method \"%s\" with @Remote " +
                                            "can't be same name and same parameter count", method.getName()));
                                }
                                CtClass returnCc = method.getReturnType();
                                checkPrimitiveType(method.getName(), returnCc);
                                CtClass[] paraTypeCts = method.getParameterTypes();
                                StringBuilder para = new StringBuilder();
                                if (paraTypeCts != null) {
                                    for (int j = 0; j < paraTypeCts.length; j++) {
                                        checkPrimitiveType(method.getName(), paraTypeCts[j]);
                                        para.append(String.format("(%s) (args[%s])", paraTypeCts[j].getName(), j));
                                        if (j != paraTypeCts.length - 1) {
                                            para.append(",");
                                        }
                                    }
                                }
                                if (!"void".equals(returnCc.getName())) {
                                    allIfStr.append(String.format(
                                            "if (\"%s\".equals(methodName)) { return ((%s)instance).%s(%s); }",
                                            method.getName() + "_$$_" + method.getParameterTypes().length,
                                            serviceCc.getName(), method.getName(), para));
                                } else {
                                    allIfStr.append(String.format(
                                            "if (\"%s\".equals(methodName)) { ((%s)instance).%s(%s); return null; }",
                                            method.getName() + "_$$_" + method.getParameterTypes().length,
                                            serviceCc.getName(), method.getName(), para));
                                }
                            }
                        }
                        method2 = String.format(
                                "public java.lang.Object execute(Object instance, String methodName, Object[] " +
                                "args) {" +
                                "%s" +
                                "throw " +
                                "new com.didi.drouter.store.IRouterProxy.RemoteMethodMatchException();" +
                                "}",
                                allIfStr);
                    }
                    if (method1 != null || method2 != null) {
                        CtClass proxyInterface = pool.get("com.didi.drouter.store.IRouterProxy");
                        String path = PROXY + serviceCc.getName().replace(".", "_");
                        if (pool.getOrNull(path) == null) {
                            proxyCc = pool.makeClass(path);
                            proxyCc.addInterface(proxyInterface);
                            generatorClass(routerDir, proxyCc,
                                    method1 == null ? METHOD1 : method1,
                                    method2 == null ? METHOD2 : method2);
                        }
                    }

                    StringBuilder itemBuilder = new StringBuilder();
                    itemBuilder.append("    put(");
                    itemBuilder.append(functionCc.getName());
                    itemBuilder.append(".class, com.didi.drouter.store.RouterMeta.build(");
                    itemBuilder.append("com.didi.drouter.store.RouterMeta.SERVICE)");
                    itemBuilder.append(".assembleService(");
                    itemBuilder.append(serviceCc.getName());
                    itemBuilder.append(".class, ");
                    itemBuilder.append(proxyCc != null ? "new " + proxyCc.getName() + "()" : "null");
                    itemBuilder.append(", \"");
                    itemBuilder.append(alias);
                    itemBuilder.append("\",");
                    itemBuilder.append(featureMatchCc != null ? "new " + featureMatchCc.getName() + "()" : "null");
                    itemBuilder.append(",");
                    itemBuilder.append(priority);
                    itemBuilder.append(",");
                    itemBuilder.append(cacheValue);
                    itemBuilder.append(")");
                    itemBuilder.append(", data);\n");
                    items.add(itemBuilder.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Class: === " + serviceCc.getName() + " ===\nCause: " + e.getMessage());
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

    private void completeMatchMethod(CtClass serviceCc, CtClass featureCc,
                                     StringBuilder featureBuilder) throws Exception {
        CtField[] serviceFields = serviceCc.getDeclaredFields();
        CtField[] featureFields = featureCc.getDeclaredFields();
        for (CtField serviceField : serviceFields) {
            if (serviceField.hasAnnotation(Assign.class)) {

                CtClass fieldTypeCc = serviceField.getType();  //the type of property: int,int[],
                //Logger.v("type: " + fieldTypeCc.getName());

                // remain basic,string,their array
                if (fieldTypeCc.getName().contains(".") &&
                        !"java.lang.String".equals(fieldTypeCc.getName()) &&
                        !"java.lang.String[]".equals(fieldTypeCc.getName()) ||
                        fieldTypeCc.getName().contains("[][]")) {
                    throw new Exception("@Assign should use correct type: " +
                            serviceCc.getSimpleName() + "." + serviceField.getName());
                }
                if (!(Modifier.isStatic(serviceField.getModifiers()) &&
                        Modifier.isPublic(serviceField.getModifiers()) &&
                        Modifier.isFinal(serviceField.getModifiers()))) {
                    throw new Exception("@Assign should use \"public static final\" type: " +
                            serviceCc.getSimpleName() + "." + serviceField.getName());
                }

                Assign property = (Assign) serviceField.getAnnotation(Assign.class);
                String propertyName = TextUtil.isEmpty(property.name()) ?
                        serviceField.getName() : property.name();

                Object propertyValue = null;
                if (fieldTypeCc.getName().contains("[]")) {
                    Class<?> cls = StoreUtil.getClass(serviceCc, classLoader);
                    Field field = cls.getField(serviceField.getName());
                    if (field != null) {
                        propertyValue = field.get(null);
                    }
                } else {
                    propertyValue = serviceField.getConstantValue();  //char is regard as int
                }

                boolean isHasMatch = false;
                for (int j = 0; j < featureFields.length; j++) {
                    // check property name match first, and then its type must be match
                    if (featureFields[j] != null &&
                            featureFields[j].getName().equals(propertyName)) {
                        if (!serviceField.getType().getName().equals(featureFields[j].getType().getName()) &&
                                !serviceField.getType().getName().equals(featureFields[j].getType().getName() + "[]")) {
                            throw new Exception("@Assign field type should be matched with feature field type: " +
                                    featureCc.getSimpleName() + "." + featureFields[j].getName());
                        }
                        featureFields[j] = null;
                        isHasMatch = true;
                    }
                }
                // support multi feature, so Assign may be redundant, then ignore it.
                if (!isHasMatch) {
                    continue;
                }

                // each Assign generator one && sentence
                featureBuilder.append(" &&\n      ");
                if (fieldTypeCc.getName().startsWith("java.lang.String")) {
                    // string or string array
                    featureBuilder.append("(");
                    if (propertyValue instanceof String[]) {
                        for (String value : (String[]) propertyValue) {
                            appendFeatureString(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else {
                        appendFeatureString(featureBuilder, featureCc.getName(), propertyName, propertyValue);
                    }
                    if (featureBuilder.toString().endsWith(" || ")) {
                        featureBuilder.delete(featureBuilder.length() - 4, featureBuilder.length());
                    }
                    featureBuilder.append(")");
                } else {
                    // basic or basic array
                    featureBuilder.append("(");
                    if (propertyValue instanceof int[]) {
                        for (int value : (int[]) propertyValue) {
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else if (propertyValue instanceof short[]) {
                        for (short value : (short[]) propertyValue) {
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else if (propertyValue instanceof long[]) {
                        for (long value : (long[]) propertyValue) {
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else if (propertyValue instanceof byte[]) {
                        for (byte value : (byte[]) propertyValue) {
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else if (propertyValue instanceof char[]) {
                        for (char value : (char[]) propertyValue) {
                            // must has single quotes as '1', then can be get its value
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else if (propertyValue instanceof float[]) {
                        for (float value : (float[]) propertyValue) {
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else if (propertyValue instanceof double[]) {
                        for (double value : (double[]) propertyValue) {
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else if (propertyValue instanceof boolean[]) {
                        for (boolean value : (boolean[]) propertyValue) {
                            appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, value);
                        }
                    } else {
                        appendFeatureBasic(featureBuilder, featureCc.getName(), propertyName, propertyValue);
                    }
                    if (featureBuilder.toString().endsWith(" || ")) {
                        featureBuilder.delete(featureBuilder.length() - 4, featureBuilder.length());
                    }
                    featureBuilder.append(")");
                }
            }
        }
        // check the rest of property, whether has no matcher.
        for (CtField beanField : featureFields) {
            if (beanField != null) {
                throw new Exception("should use @Assign to match this field: " +
                        featureCc.getSimpleName() + "." + beanField.getName());
            }
        }
    }

    @Override
    public boolean include(CtClass ct) {
        return ct.hasAnnotation(Service.class);
    }

    private void appendFeatureBasic(StringBuilder featureBuilder, String featureClass,
                                    String propertyName, Object value) {
        featureBuilder.append("((");
        featureBuilder.append(featureClass);
        featureBuilder.append(")obj).");
        featureBuilder.append(propertyName);
        featureBuilder.append(" == ");
        featureBuilder.append(value instanceof Character ? "'" + value + "'" : value);
        featureBuilder.append(" || ");
    }

    private void appendFeatureString(StringBuilder featureBuilder, String featureClass,
                                     String propertyName, Object value) {
        featureBuilder.append("android.text.TextUtils.equals(");
        featureBuilder.append("((");
        featureBuilder.append(featureClass);
        featureBuilder.append(")obj).");
        featureBuilder.append(propertyName);
        featureBuilder.append(", ");
        featureBuilder.append(value == null ? "" : "\"");
        featureBuilder.append(value);    //null->"null"
        featureBuilder.append(value == null ? "" : "\"");
        featureBuilder.append(")");
        featureBuilder.append(" || ");
    }

    private void checkPrimitiveType(String method, CtClass clz) throws Exception {
        boolean check =
                "byte".equals(clz.getName()) ||
                "short".equals(clz.getName()) ||
                "int".equals(clz.getName()) ||
                "long".equals(clz.getName()) ||
                "float".equals(clz.getName()) ||
                "double".equals(clz.getName()) ||
                "char".equals(clz.getName()) ||
                "boolean".equals(clz.getName());
        if (check) {
            throw new Exception(String.format("The type \"%s\" in method \"%s\" with @Remote " +
                            "can't use primitive type",
                    clz.getName(), method));
        }
    }
}
