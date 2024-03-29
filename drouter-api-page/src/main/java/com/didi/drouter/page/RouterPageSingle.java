package com.didi.drouter.page;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
        fragment = createFragment(bean.getPageUri());
        putArgsForFragment(fragment, bean.getPageInfo());
        manager.beginTransaction().replace(containerId, fragment).commitAllowingStateLoss();
        notifyPageChanged(bean, IPageObserver.CHANGED_BY_REPLACE, false);
    }

    @Override
    public void popPage() {
        if (fragment != null) {
            manager.beginTransaction().remove(fragment).commitAllowingStateLoss();
            notifyPageChanged(new IPageBean.EmptyPageBean(), IPageObserver.CHANGED_BY_POP, false);
            fragment = null;
        }
    }
}
