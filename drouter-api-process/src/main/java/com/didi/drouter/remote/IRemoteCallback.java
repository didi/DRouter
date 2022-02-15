package com.didi.drouter.remote;

import android.os.IBinder;
import android.os.IInterface;

import androidx.annotation.NonNull;

import com.didi.drouter.api.Extend;

/**
 * Created by gaowei on 2019/2/27
 *
 * IRemoteCallback in server can be stored and reused indefinitely.
 */
public interface IRemoteCallback {

    /**
     * @param data the data of return to client.
     */
    void callback(Object... data);


    //----------------------------------------------------------------------------------------//
    abstract class Type0 extends Base {
        public abstract void callback();
        @Override @Deprecated
        public void callback(Object... data) {
            callback();
        }
    }

    abstract class Type1<Param1> extends Base {
        public abstract void callback(Param1 p1);
        @Override @Deprecated @SuppressWarnings("unchecked")
        // compatible with old interface, transform to new
        public void callback(Object... data) {
            // force type cast
            if (data == null || data.length == 0) callback((Param1) null);
            else callback((Param1) data[0]);
        }
    }

    abstract class Type2<Param1, Param2> extends Base {
        public abstract void callback(Param1 p1, Param2 p2);
        @Override @Deprecated @SuppressWarnings("unchecked")
        public void callback(Object... data) {
            // force type cast
            if (data == null || data.length == 0) callback(null, null);
            else callback((Param1) data[0], (Param2) data[1]);
        }
    }

    abstract class Type3<Param1, Param2, Param3> extends Base {
        public abstract void callback(Param1 p1, Param2 p2, Param3 p3);
        @Override @Deprecated @SuppressWarnings("unchecked")
        public void callback(Object... data) {
            // force type cast
            if (data == null || data.length == 0) callback(null, null, null);
            else callback((Param1) data[0], (Param2) data[1], (Param3) data[2]);
        }
    }

    abstract class Type4<Param1, Param2, Param3, Param4> extends Base {
        public abstract void callback(Param1 p1, Param2 p2, Param3 p3, Param4 p4);
        @Override @Deprecated @SuppressWarnings("unchecked")
        public void callback(Object... data) {
            // force type cast
            if (data == null || data.length == 0) callback(null, null, null, null);
            else callback((Param1) data[0], (Param2) data[1], (Param3) data[2], (Param4) data[3]);
        }
    }

    abstract class Type5<Param1, Param2, Param3, Param4, Param5> extends Base {
        public abstract void callback(Param1 p1, Param2 p2, Param3 p3, Param4 p4, Param5 p5);
        @Override @Deprecated @SuppressWarnings("unchecked")
        public void callback(Object... data) {
            // force type cast
            if (data == null || data.length == 0) callback(null, null, null, null, null);
            else callback((Param1) data[0], (Param2) data[1], (Param3) data[2], (Param4) data[3], (Param5) data[4]);
        }
    }

    abstract class TypeN extends Base {

    }

    //----------------------------------------------------------------------------------------//
    // TODO 有时间重构一下这个
    abstract class Base implements IInterface, IRemoteCallback {
        IBinder binder;
        void setBinder(@NonNull IBinder binder) {
            this.binder = binder;
        }
        @NonNull
        public IBinder asBinder() {
            return binder;
        }
        @Extend.Thread
        public int mode() {
            return Extend.Thread.MAIN;
        }
    }
}
