package com.didi.drouter.page;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gaowei on 2020/4/1
 */
public class RouterPageStack extends RouterPageAbs {

    private final FragmentManager manager;
    private final int containerId;
    private final List<Fragment> fragments = new ArrayList<>();
    private final List<IPageBean> curInfoList = new ArrayList<>();

    public RouterPageStack(FragmentManager manager, @IdRes int containerId) {
        this.manager = manager;
        this.containerId = containerId;
    }

    @Override
    public void showPage(@NonNull IPageBean bean) {
        Fragment fragment = newFragment(bean.getPageUri());
        addArgsForFragment(fragment, bean.getPageInfo());
        manager.beginTransaction().add(containerId, fragment).commitAllowingStateLoss();
        notifyPageChanged(bean);
        fragments.add(fragment);
        curInfoList.add(bean);
    }

    @Override
    public void popPage() {
        if (!fragments.isEmpty()) {
            int index = fragments.size() - 1;
            Fragment fragment = fragments.remove(index);
            curInfoList.remove(index);
            manager.beginTransaction().remove(fragment).commitAllowingStateLoss();
            notifyPageChanged(index - 1 >= 0 && index - 1 < curInfoList.size() ?
                    curInfoList.get(index - 1) : new IPageBean.EmptyPageBean());
        }
    }

    @NonNull
    @Override
    public IPageBean getCurPage() {
        return super.getCurPage();
    }
}
