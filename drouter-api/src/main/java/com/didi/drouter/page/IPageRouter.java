package com.didi.drouter.page;

import android.arch.lifecycle.LifecycleOwner;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by gaowei on 2020/4/1
 */
public interface IPageRouter {

    void showPage(@NonNull IPageBean bean);

    void popPage();

    @NonNull IPageBean getCurPage();

    Bundle execute(String method, Bundle bundle);

    void addPageObserver(IPageObserver listener, @Nullable LifecycleOwner owner);

    void removePageObserver(IPageObserver listener);

    interface IPageObserver {
        void onPageChange(@NonNull IPageBean from, @NonNull IPageBean to);
    }
}
