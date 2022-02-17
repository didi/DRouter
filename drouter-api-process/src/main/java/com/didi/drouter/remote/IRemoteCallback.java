package com.didi.drouter.remote;

import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;

import com.didi.drouter.api.Extend;

/**
 * Created by gaowei on 2019/2/27
 *
 * IRemoteCallback in server can be stored and reused indefinitely.
 */
@SuppressWarnings("unchecked")
public interface IRemoteCallback {

    abstract class Type0 extends Base {
        public abstract void callback();
        @Override
        void callback(Object... data) {
            callback();
        }
    }

    abstract class Type1<Param1> extends Base {
        public abstract void callback(Param1 p1);
        @Override
        public void callback(Object... data) {
            if (data == null || data.length == 0) callback((Param1) null);
            else callback((Param1) data[0]);
        }
    }

    abstract class Type2<Param1, Param2> extends Base {
        public abstract void callback(Param1 p1, Param2 p2);
        @Override
        void callback(Object... data) {
            if (data == null || data.length == 0) callback(null, null);
            else callback((Param1) data[0], (Param2) data[1]);
        }
    }

    abstract class Type3<Param1, Param2, Param3> extends Base {
        public abstract void callback(Param1 p1, Param2 p2, Param3 p3);
        @Override
        public void callback(Object... data) {
            if (data == null || data.length == 0) callback(null, null, null);
            else callback((Param1) data[0], (Param2) data[1], (Param3) data[2]);
        }
    }

    abstract class Type4<Param1, Param2, Param3, Param4> extends Base {
        public abstract void callback(Param1 p1, Param2 p2, Param3 p3, Param4 p4);
        @Override
        public void callback(Object... data) {
            if (data == null || data.length == 0) callback(null, null, null, null);
            else callback((Param1) data[0], (Param2) data[1], (Param3) data[2], (Param4) data[3]);
        }
    }

    abstract class Type5<Param1, Param2, Param3, Param4, Param5> extends Base {
        public abstract void callback(Param1 p1, Param2 p2, Param3 p3, Param4 p4, Param5 p5);
        @Override
        public void callback(Object... data) {
            if (data == null || data.length == 0) callback(null, null, null, null, null);
            else callback((Param1) data[0], (Param2) data[1], (Param3) data[2], (Param4) data[3], (Param5) data[4]);
        }
    }

    abstract class TypeN extends Base {
        @Override
        protected abstract void callback(Object... data);
    }

    abstract class Base implements IInterface, IRemoteCallback {
        String authority;
        IBinder binder;
        void callback(Object... data) {
        }

        @NonNull public IBinder asBinder() {
            return binder;
        }

        @Extend.Thread protected int thread() {
            return Extend.Thread.POSTING;
        }

        // this callback object lifecycle, default it will depend on server recycle.
        protected Lifecycle lifecycle() {
            return null;
        }

        protected void onServerDead() {
        }
    }
}
