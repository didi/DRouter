package com.didi.drouter.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.api.Strategy;

import java.lang.ref.WeakReference;

/**
 * Created by gaowei on 2022/02/15
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IRemoteBridge {

    <T> T getService(@NonNull Strategy strategy, WeakReference<LifecycleOwner> lifecycle,
                     Class<T> serviceClass, String alias, Object feature, @Nullable Object... constructor);
}
