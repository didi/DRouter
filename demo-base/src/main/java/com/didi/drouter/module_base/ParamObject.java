package com.didi.drouter.module_base;

import android.support.annotation.Keep;

import com.didi.drouter.utils.JsonConverter;

/**
 * Created by gaowei on 2018/11/2
 */
@Keep
public class ParamObject {

    public int a;

    public short b;

    public long c;

    public byte d;

    public char e;

    public float f;

    public double g;

    public boolean h;

    public String i;

    public String j;

    @Override
    public String toString() {
        return "TestBean object gson: " + JsonConverter.toString(this);
    }
}
