package com.didi.drouter.remote;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.ArraySet;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.utils.JsonConverter;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterLogger;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2019/1/25
 */
class RemoteStream {

    static Object transform(Object o) {
        if (isParcelable(o)) {
            return o;
        } else if (o.getClass().isArray()) {
            return new ArrayParcelable(o);
        } else if (o instanceof Map) {
            return new MapParcelable(o);
        } else if (o instanceof Collection) {
            return new CollectionParcelable(o);
        } else if (o instanceof IRemoteCallback) {
            return new CallbackParcelable(o);
        } else {
            return new ObjectParcelable(o);
        }
    }

    static Object reverse(Object o) {
        if (o instanceof ArrayParcelable) {
            return ((ArrayParcelable) o).getArray();
        } else if (o instanceof MapParcelable) {
            return ((MapParcelable) o).getMap();
        } else if (o instanceof CollectionParcelable) {
            return ((CollectionParcelable) o).getCollection();
        } else if (o instanceof ObjectParcelable) {
            return ((ObjectParcelable) o).getObject();
        } else if (o instanceof CallbackParcelable) {
            return ((CallbackParcelable) o).getObject();
        } else {
            return o;
        }
    }

    private static boolean isParcelable(Object object) {
        return  object == null ||
                object instanceof Boolean || object instanceof boolean[] ||
                object instanceof Byte || object instanceof byte[] ||
                object instanceof Character || object instanceof char[] ||
                object instanceof Short || object instanceof short[] ||
                object instanceof Integer || object instanceof int[] ||
                object instanceof Long || object instanceof long[] ||
                object instanceof Float || object instanceof float[] ||
                object instanceof Double || object instanceof double[] ||
                object instanceof CharSequence || object instanceof CharSequence[] ||
                object instanceof Parcelable || object instanceof Parcelable[] ||
                object instanceof Class || object instanceof IBinder;
    }

    static class ArrayParcelable implements Parcelable {

        Object[] array;

        ArrayParcelable(Object o) {
            array = (Object[]) o;
        }

        ArrayParcelable(Parcel in) {
            Class<?> clz = (Class<?>) in.readSerializable();
            Object[] tmp = in.readArray(getClass().getClassLoader());
            array = (Object[]) Array.newInstance(clz, tmp.length);
            for (int i = 0; i < tmp.length; i++) {
                array[i] = reverse(tmp[i]);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            Object[] tmp = new Object[array.length];  //type may change after transform
            for (int i = 0; i < array.length; i++) {
                tmp[i] = transform(array[i]);
            }
            writeSerializable(dest, array.getClass().getComponentType());
            dest.writeArray(tmp);   //no shell type
        }

        Object[] getArray() {
            return array;
        }

        public static final Creator<ArrayParcelable> CREATOR = new Creator<ArrayParcelable>() {
            @Override
            public ArrayParcelable createFromParcel(Parcel in) {
                return new ArrayParcelable(in);
            }

            @Override
            public ArrayParcelable[] newArray(int size) {
                return new ArrayParcelable[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    static class MapParcelable implements Parcelable {

        Map<Object, Object> map;

        MapParcelable(Object o) {
            map = (Map<Object, Object>) o;
        }

        MapParcelable(Parcel in) {
            Class<?> clz = (Class<?>) in.readSerializable();
            if (clz == HashMap.class) {
                map = new HashMap<>();
            } else if (clz == ArrayMap.class) {
                map = new ArrayMap<>();
            } else if (clz == ConcurrentHashMap.class) {
                map = new ConcurrentHashMap<>();
            } else {
                map = (Map<Object, Object>) ReflectUtil.getInstance(clz);
            }
            Map<Object, Object> tmp = in.readHashMap(getClass().getClassLoader());
            for (Map.Entry<Object, Object> entry : tmp.entrySet()) {
                map.put(reverse(entry.getKey()), reverse(entry.getValue()));
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            Class<?> clz = map.getClass();
            Map<Object, Object> tmp = new HashMap<>();
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                tmp.put(transform(entry.getKey()), transform(entry.getValue()));
            }
            writeSerializable(dest, clz);
            dest.writeMap(tmp);
        }

        Map<Object, Object> getMap() {
            return map;
        }

        public static final Creator<MapParcelable> CREATOR = new Creator<MapParcelable>() {
            @Override
            public MapParcelable createFromParcel(Parcel in) {
                return new MapParcelable(in);
            }

            @Override
            public MapParcelable[] newArray(int size) {
                return new MapParcelable[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }
    }

    @SuppressWarnings("unchecked")
    static class CollectionParcelable implements Parcelable {

        Collection<Object> collection;

        CollectionParcelable(Object o) {
            collection = (Collection<Object>) o;
        }

        CollectionParcelable(Parcel in) {
            Class<?> clz = (Class<?>) in.readSerializable();
            if (clz == ArrayList.class) {
                collection = new ArrayList<>();
            } else if (clz == HashSet.class) {
                collection = new HashSet<>();
            } else if (clz == ArraySet.class) {
                collection = new ArraySet<>();
            } else if (clz == LinkedList.class) {
                collection = new LinkedList<>();
            } else {
                collection = (Collection<Object>) ReflectUtil.getInstance(clz);
            }
            List<Object> tmp = in.readArrayList(getClass().getClassLoader());
            for (Object object : tmp) {
                collection.add(reverse(object));
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            Class<?> clz = collection.getClass();
            List<Object> tmp = new ArrayList<>();
            for (Object object : collection) {
                tmp.add(transform(object));
            }
            writeSerializable(dest, clz);
            dest.writeList(tmp);
        }

        Collection<Object> getCollection() {
            return collection;
        }

        public static final Creator<CollectionParcelable> CREATOR = new Creator<CollectionParcelable>() {
            @Override
            public CollectionParcelable createFromParcel(Parcel in) {
                return new CollectionParcelable(in);
            }

            @Override
            public CollectionParcelable[] newArray(int size) {
                return new CollectionParcelable[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }
    }

    static class ObjectParcelable implements Parcelable {

        Object object;

        ObjectParcelable(Object object) {
            this.object = object;
        }

        ObjectParcelable(Parcel in) {
            int type = in.readInt();
            if (type == 0) {
                object = DRouter.getContext();
            } else {
                Class<?> clz = (Class<?>) in.readSerializable();
                object = JsonConverter.toObject(in.readString(), clz);
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            if (object instanceof Context) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                writeSerializable(dest, object.getClass());
                dest.writeString(JsonConverter.toString(object));
            }
        }

        Object getObject() {
            return object;
        }

        public static final Creator<ObjectParcelable> CREATOR = new Creator<ObjectParcelable>() {
            @Override
            public ObjectParcelable createFromParcel(Parcel in) {
                return new ObjectParcelable(in);
            }

            @Override
            public ObjectParcelable[] newArray(int size) {
                return new ObjectParcelable[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }
    }

    static class CallbackParcelable implements Parcelable {

        static Map<IRemoteCallback, IClientService> callbackPool =
                Collections.synchronizedMap(new WeakHashMap<IRemoteCallback, IClientService>());

        IBinder binder;

        CallbackParcelable(Object object) {
            final IRemoteCallback callback = (IRemoteCallback) object;
            IClientService callbackBinder = callbackPool.get(callback);
            if (callbackBinder == null) {
                callbackBinder = new IClientService.Stub() {
                    @Override
                    public RemoteResult callback(RemoteCommand callbackCommand) throws RemoteException {
                        RouterLogger.getCoreLogger().d("[Client] receive callback success and start invoke");
                        callback.callback(callbackCommand.callbackData);
                        return null;
                    }
                };
                callbackPool.put(callback, callbackBinder);
            }
            binder = callbackBinder.asBinder();
        }

        CallbackParcelable(Parcel in) {
            binder = in.readStrongBinder();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStrongBinder(binder);
        }

        Object getObject() {
            for (Map.Entry<IRemoteCallback, IClientService> entry : callbackPool.entrySet()) {
                if (entry.getValue().asBinder() == binder) {
                    return entry.getKey();
                }
            }
            final IClientService callbackService = IClientService.Stub.asInterface(binder);
            IRemoteCallback callback = new IRemoteCallback() {
                @Override
                public void callback(Object... data) throws RemoteException {
                    if (data == null) {
                        data = new Object[] {null};
                    }
                    RouterLogger.getCoreLogger().w("[Service] remote callback start invoke");
                    RemoteCommand callbackCommand = new RemoteCommand(RemoteCommand.SERVICE_CALLBACK);
                    callbackCommand.callbackData = data;
                    try {
                        callbackService.callback(callbackCommand);
                    } catch (RemoteException e) {
                        RouterLogger.getCoreLogger().e("[Service] command callback Exception %s", e);
                        throw e;
                    }
                }
            };
            callbackPool.put(callback, callbackService);
            return callback;
        }

        public static final Creator<CallbackParcelable> CREATOR = new Creator<CallbackParcelable>() {
            @Override
            public CallbackParcelable createFromParcel(Parcel in) {
                return new CallbackParcelable(in);
            }

            @Override
            public CallbackParcelable[] newArray(int size) {
                return new CallbackParcelable[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }
    }

    private static void writeSerializable(Parcel dest, Class<?> clz) {
        boolean isSerializable = !((clz.isLocalClass() || clz.isMemberClass() || clz.isAnonymousClass()) &&
                (clz.getModifiers() & Modifier.STATIC) == 0);
        if (isSerializable) {
            dest.writeSerializable(clz);
        } else {
            // for instance
            throw new IllegalArgumentException(
                    String.format("non static inner class \"%s\" can not be serialized", clz.getName()));
        }
    }
}




