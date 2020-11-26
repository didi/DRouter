package com.didi.drouter.utils;

import com.google.gson.Gson;

/**
 * Created by gaowei on 2018/11/2
 */
public class JsonConverter {

    private static IConvert jsonConvert;

    public static void setConverter(IConvert convert) {
        jsonConvert = convert;
    }

    public static String toString(Object object) {
        check();
        return jsonConvert.toJson(object);
    }

    /**
     * Avoid RCE "com.android.internal.util.VirtualRefBasePtr", "{'mNativePtr':3735928551}"
     */
    public static <T> T toObject(String json, Class<T> cls) {
        check();
        T t = null;
        if (cls != null && !cls.getName().contains("com.android.internal")) {
            t = jsonConvert.fromJson(json, cls);
        }
        if (t == null) {
            RouterLogger.getCoreLogger().w(
                    "Json %s convert to object \"%s\" error",
                    json, cls != null ? cls.getSimpleName() : null);
        }
        return t;
    }

    private static void check() {
        if (jsonConvert == null) {
            jsonConvert = new InnerConvert();
        }
    }

    public interface IConvert {

        String toJson(Object obj);

        <T> T fromJson(String json, Class<T> classOfT);
    }

    private static class InnerConvert implements IConvert {

        private final Gson gson = new Gson();

        @Override
        public String toJson(Object obj) {
            return gson.toJson(obj);
        }

        @Override
        public <T> T fromJson(String json, Class<T> classOfT) {
            return gson.fromJson(json, classOfT);
        }
    }
}
