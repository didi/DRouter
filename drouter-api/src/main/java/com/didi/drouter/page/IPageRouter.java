package com.didi.drouter.page;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2020/4/1
 */
public interface IPageRouter {

    /**
     * @param bean PageUri can be duplicated
     */
    void showPage(@NonNull IPageBean bean);

    void popPage();

    @NonNull IPageBean getCurPage();

    Bundle execute(String method, Bundle bundle);

    void addPageObserver(IPageObserver listener, boolean sticky, @Nullable LifecycleOwner owner);

    void removePageObserver(IPageObserver listener);

    interface IPageObserver {

        // for change type
        int CHANGED_BY_SCROLL = 0;            // view pager hand scroll
        int CHANGED_BY_SHOW = 1;              // view pager show method
        int CHANGED_BY_POP = 2;               // single or stack pop
        int CHANGED_BY_REPLACE = 3;           // single show or stack replace

        void onPageChange(@NonNull IPageBean from, @NonNull IPageBean to, int changeType);
    }
}
