package com.didi.drouter.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by gaowei on 2018/9/4
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ReflectUtil {

    // not support non static inner class
    public static Object getInstance(@NonNull Class<?> implClass, @Nullable Object... params) {
        try {
            // null indicates a null param
            if (params == null) {
                params = new Object[] {null};
            }
            RouterLogger.getCoreLogger().w(
                    "ReflectUtil create instance \"%s\" with params number %s by reflect",
                    implClass.getSimpleName(),
                    params.length);
            if (params.length == 0) {
                return implClass.newInstance();
            }
            Constructor<?>[] constructors = implClass.getConstructors();
            if (constructors != null) {
                for (Constructor<?> constructor : constructors) {
                    Class<?>[] classes = constructor.getParameterTypes();
                    if (isParameterTypeMatch(classes, params)) {
                        return constructor.newInstance(params);
                    }
                }
            }
            RouterLogger.getCoreLogger().e("ReflectUtil \"%s\" getInstance no match and return \"null\"", implClass);
        } catch (Exception e) {
            RouterLogger.getCoreLogger().e("ReflectUtil \"%s\" getInstance Exception: %s", implClass, e);
        }
        return null;
    }

    public static Object invokeMethod(Object instance, String methodName, @Nullable Object[] params) throws Exception {
        RouterLogger.getCoreLogger().w("ReflectUtil invoke method \"%s\" by reflect", methodName);
        if (params == null || params.length == 0) {
            Method method = instance.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(instance);
        }
        Method[] methods = instance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class<?>[] classes = method.getParameterTypes();
                if (isParameterTypeMatch(classes, params)) {
                    method.setAccessible(true);
                    return method.invoke(instance, params);
                }
            }
        }
        throw new Exception("ReflectUtil invokeMethod no match");
    }

    private static boolean isParameterTypeMatch(@NonNull Class<?>[] target, @NonNull Object[] params) {
        if (target.length != params.length) return false;
        for (int i = 0; i < target.length; i++) {
            Class<?> host = target[i];
            Class<?> client = params[i] != null ? params[i].getClass() : null;
            boolean typeMatch;
            if (client == null) {
                typeMatch = !host.isPrimitive();
            } else {
                typeMatch = host == client ||
                        host.isAssignableFrom(client) ||
                        transform(host) == transform(client);
            }
            if (!typeMatch) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> transform(Class<?> clz) {
        if (clz == Byte.class)
            clz = byte.class;
        else if (clz == Short.class)
            clz = short.class;
        else if (clz == Integer.class)
            clz = int.class;
        else if (clz == Long.class)
            clz = long.class;
        else if (clz == Float.class)
            clz = float.class;
        else if (clz == Double.class)
            clz = double.class;
        else if (clz == Character.class)
            clz = char.class;
        else if (clz == Boolean.class)
            clz = boolean.class;
        return clz;
    }


}