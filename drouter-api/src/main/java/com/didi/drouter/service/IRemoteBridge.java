package com.didi.drouter.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.lifecycle.Lifecycle;

import com.didi.drouter.api.Strategy;

/**
 * Created by gaowei on 2022/02/15
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface IRemoteBridge {

    <T> T getService(@NonNull Strategy strategy, Lifecycle lifecycle,
                     Class<T> serviceClass, String alias, Object feature, @Nullable Object... constructor);
}
