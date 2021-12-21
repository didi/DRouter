package com.didi.drouter.router;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;
import android.util.SparseArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by gaowei on 2018/8/31
 */
@SuppressWarnings("unchecked")
class DataExtras<T> {

    private final T self;

    public Bundle extra = new Bundle();

    public Map<String, Object> addition = new ArrayMap<>();

    public DataExtras() {
        self = (T) this;
    }

    public @NonNull Bundle getExtra() {
        return extra;
    }

    public @NonNull Map<String, Object> getAddition() {
        return addition;
    }

    public T putExtra(String key, String value) {
        extra.putString(key, value);
        return self;
    }

    public T putExtra(String key, boolean value) {
        extra.putBoolean(key, value);
        return self;
    }

    public T putExtra(String key, byte value) {
        extra.putByte(key, value);
        return self;
    }

    public T putExtra(String key, char value) {
        extra.putChar(key, value);
        return self;
    }

    public T putExtra(String key, short value) {
        extra.putShort(key, value);
        return self;
    }

    public T putExtra(String key, int value) {
        extra.putInt(key, value);
        return self;
    }

    public T putExtra(String key, long value) {
        extra.putLong(key, value);
        return self;
    }

    public T putExtra(String key, float value) {
        extra.putFloat(key, value);
        return self;
    }

    public T putExtra(String key, double value) {
        extra.putDouble(key, value);
        return self;
    }

    public T putExtra(String key, CharSequence value) {
        extra.putCharSequence(key, value);
        return self;
    }

    public T putExtra(String key, Parcelable value) {
        extra.putParcelable(key, value);
        return self;
    }

    public T putExtra(String key, Serializable value) {
        extra.putSerializable(key, value);
        return self;
    }

    public T putExtra(String key, String[] value) {
        extra.putStringArray(key, value);
        return self;
    }

    public T putExtra(String key, boolean[] value) {
        extra.putBooleanArray(key, value);
        return self;
    }

    public T putExtra(String key, byte[] value) {
        extra.putByteArray(key, value);
        return self;
    }

    public T putExtra(String key, char[] value) {
        extra.putCharArray(key, value);
        return self;
    }

    public T putExtra(String key, short[] value) {
        extra.putShortArray(key, value);
        return self;
    }

    public T putExtra(String key, int[] value) {
        extra.putIntArray(key, value);
        return self;
    }

    public T putExtra(String key, long[] value) {
        extra.putLongArray(key, value);
        return self;
    }

    public T putExtra(String key, float[] value) {
        extra.putFloatArray(key, value);
        return self;
    }

    public T putExtra(String key, double[] value) {
        extra.putDoubleArray(key, value);
        return self;
    }

    public T putExtra(String key, CharSequence[] value) {
        extra.putCharSequenceArray(key, value);
        return self;
    }

    public T putExtra(String key, Parcelable[] value) {
        extra.putParcelableArray(key, value);
        return self;
    }

    public T putExtra(String key, Bundle value) {
        extra.putBundle(key, value);
        return self;
    }

    public T putParcelableArrayList(String key, ArrayList<? extends Parcelable> value) {
        extra.putParcelableArrayList(key, value);
        return self;
    }

    public T putIntegerArrayList(String key, ArrayList<Integer> value) {
        extra.putIntegerArrayList(key, value);
        return self;
    }

    public T putStringArrayList(String key, ArrayList<String> value) {
        extra.putStringArrayList(key, value);
        return self;
    }

    public T putCharSequenceArrayList(String key, ArrayList<CharSequence> value) {
        extra.putCharSequenceArrayList(key, value);
        return self;
    }

    public T putParcelableSparseArray(String key, SparseArray<? extends Parcelable> value) {
        extra.putSparseParcelableArray(key, value);
        return self;
    }

    public T putExtras(Bundle bundle) {
        extra.putAll(bundle);
        return self;
    }

    public boolean getBoolean(String key) {
        return extra.getBoolean(key);
    }

    public Byte getByte(String key) {
        return extra.getByte(key);
    }

    public char getChar(String key) {
        return extra.getChar(key);
    }

    public short getShort(String key) {
        return extra.getShort(key);
    }

    public int getInt(String key) {
        return extra.getInt(key);
    }

    public long getLong(String key) {
        return extra.getLong(key);
    }

    public float getFloat(String key) {
        return extra.getFloat(key);
    }

    public double getDouble(String key) {
        return extra.getDouble(key);
    }

    public String getString(String key) {
        return extra.getString(key);
    }

    public Bundle getBundle(String key) {
        return extra.getBundle(key);
    }

    public CharSequence getCharSequence(String key) {
        return extra.getCharSequence(key);
    }

    public <M extends Parcelable> M getParcelable(String key) {
        return extra.getParcelable(key);
    }

    public Serializable getSerializable(String key) {
        return extra.getSerializable(key);
    }

    public String[] getStringArray(String key) {
        return extra.getStringArray(key);
    }

    public boolean[] getBooleanArray(String key) {
        return extra.getBooleanArray(key);
    }

    public short[] getShortArray(String key) {
        return extra.getShortArray(key);
    }

    public int[] getIntArray(String key) {
        return extra.getIntArray(key);
    }

    public long[] getLongArray(String key) {
        return extra.getLongArray(key);
    }

    public byte[] getByteArray(String key) {
        return extra.getByteArray(key);
    }

    public char[] getCharArray(String key) {
        return extra.getCharArray(key);
    }

    public float[] getFloatArray(String key) {
        return extra.getFloatArray(key);
    }

    public double[] getDoubleArray(String key) {
        return extra.getDoubleArray(key);
    }

    public CharSequence[] getCharSequenceArray(String key) {
        return extra.getCharSequenceArray(key);
    }

    public Parcelable[] getParcelableArray(String key) {
        return extra.getParcelableArray(key);
    }

    public <M extends Parcelable> ArrayList<M> getParcelableArrayList(String key) {
        return extra.getParcelableArrayList(key);
    }

    public ArrayList<Integer> getIntegerArrayList(String key) {
        return extra.getIntegerArrayList(key);
    }

    public ArrayList<String> getStringArrayList(String key) {
        return extra.getStringArrayList(key);
    }

    public ArrayList<CharSequence> getCharSequenceArrayList(String key) {
        return extra.getCharSequenceArrayList(key);
    }

    public <M extends Parcelable> SparseArray<M> getSparseParcelableArray(String key) {
        return extra.getSparseParcelableArray(key);
    }

    /**
     * Attention!
     * This will not be put into the bundle of Activity/Fragment/View.
     * In this scenario, use {@link DataExtras#putExtra(String, String)} first.
     */
    public T putAddition(String key, Object value) {
        addition.put(key, value);
        return self;
    }

    /**
     * Attention to {@link DataExtras#putAddition(String, Object)}
     */
    public Object getAddition(String key) {
        return addition.get(key);
    }
}
