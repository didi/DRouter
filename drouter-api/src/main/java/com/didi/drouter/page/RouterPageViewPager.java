package com.didi.drouter.page;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by gaowei on 2020/4/1
 */
public class RouterPageViewPager extends RouterPageAbs {

    private final ViewPager viewPager;
    private final FragmentManager manager;
    private final ViewPagerAdapter adapter;
    private final List<String> curUriList = new ArrayList<>();
    private final List<IPageBean> curInfoList = new ArrayList<>();
    private List<String> lastUriList = new ArrayList<>();

    public RouterPageViewPager(FragmentManager manager, ViewPager container) {
        this.manager = manager;
        adapter = new ViewPagerAdapter(manager);
        viewPager = container;
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                notifyPageChanged(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public void update(IPageBean... beanList) {
        update(Arrays.asList(beanList));
    }

    @SuppressWarnings("unchecked")
    public void update(@NonNull List<IPageBean> uriList) {
        lastUriList = (List<String>) ((ArrayList<String>) curUriList).clone();
        curUriList.clear();
        curInfoList.clear();
        for (int i = 0; i < uriList.size(); i++) {
            curUriList.add(uriList.get(i).getPageUri());
            curInfoList.add(uriList.get(i));
        }
        // sync method to getCurrentItem, no trigger onPageSelected, so active it.
        adapter.notifyDataSetChanged();
        notifyPageChanged(viewPager.getCurrentItem());
    }

    private void notifyPageChanged(int position) {
        notifyPageChanged(position < curInfoList.size() ?
                curInfoList.get(position) : new IPageBean.EmptyPageBean());
    }

    @Override
    public void showPage(@NonNull IPageBean bean) {
        int index;
        if ((index = curUriList.indexOf(bean.getPageUri())) != -1) {
            viewPager.setCurrentItem(index, false);
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {

        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(final int position) {
            Fragment fragment = newFragment(curUriList.get(position));
            Bundle info = null;
            if (curInfoList.get(position) != null && curInfoList.get(position).getPageInfo() != null) {
                info = curInfoList.get(position).getPageInfo();
            }
            addArgsForFragment(fragment, info);
            return fragment;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (position < curUriList.size() && position < lastUriList.size()
                    && !curUriList.get(position).equals(lastUriList.get(position))) {
                FragmentTransaction transaction = manager.beginTransaction();
                String name = makeFragmentName(container.getId(), position);
                Fragment fragment = manager.findFragmentByTag(name);
                if (fragment != null) {
                    transaction.remove(fragment);
                    transaction.commitNowAllowingStateLoss();
                }
            }
            return super.instantiateItem(container, position);
        }

        @Override
        public int getCount() {
            return curUriList.size();
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }

    private static String makeFragmentName(int viewId, long id) {
        return "android:switcher:" + viewId + ":" + id;
    }
}
