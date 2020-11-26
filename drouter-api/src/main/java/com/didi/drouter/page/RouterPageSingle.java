package com.didi.drouter.page;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * Created by gaowei on 2020/4/1
 */
public class RouterPageSingle extends RouterPageAbs {

    private final FragmentManager manager;
    private final int containerId;
    private Fragment fragment;

    public RouterPageSingle(FragmentManager manager, @IdRes int containerId) {
        this.manager = manager;
        this.containerId = containerId;
    }

    @Override
    public void showPage(@NonNull IPageBean bean) {
        fragment = newFragment(bean.getPageUri());
        addArgsForFragment(fragment, bean.getPageInfo());
        manager.beginTransaction().replace(containerId, fragment).commitAllowingStateLoss();
        notifyPageChanged(bean);
    }

    @Override
    public void popPage() {
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commitAllowingStateLoss();
            notifyPageChanged(new IPageBean.EmptyPageBean());
            fragment = null;
        }
    }
}
