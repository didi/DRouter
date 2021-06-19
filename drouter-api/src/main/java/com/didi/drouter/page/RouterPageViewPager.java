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

import static com.didi.drouter.page.IPageRouter.IPageObserver.CHANGED_BY_SHOW;

/**
 * Created by gaowei on 2020/4/1
 */
public class RouterPageViewPager extends RouterPageAbs {

    private final ViewPager viewPager;
    private final FragmentManager fragmentManager;
    private final ViewPagerAdapter adapter;
    private final List<String> curUriList = new ArrayList<>();
    private final List<IPageBean> curInfoList = new ArrayList<>();
    private List<String> lastUriList = new ArrayList<>();
    private boolean changeByShow = false;

    public RouterPageViewPager(FragmentManager manager, ViewPager container) {
        fragmentManager = manager;
        adapter = new ViewPagerAdapter(manager);
        viewPager = container;
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // position will change but except empty to non-empty at first time.
                notifyPageChangedFromIndex(position, false,
                        changeByShow ? CHANGED_BY_SHOW : IPageObserver.CHANGED_BY_SCROLL);
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
    // bean uri may be empty
    public void update(@NonNull List<IPageBean> uriList) {
        lastUriList = (List<String>) ((ArrayList<String>) curUriList).clone();
        curUriList.clear();
        curInfoList.clear();
        for (int i = 0; i < uriList.size(); i++) {
            curUriList.add(uriList.get(i).getPageUri());
            curInfoList.add(uriList.get(i));
        }
        int lastPosition = viewPager.getCurrentItem();
        changeByShow = true;
        adapter.notifyDataSetChanged();
        changeByShow = false;
        int curPosition = viewPager.getCurrentItem();

        // notifyDataSetChanged is a sync method for getCurrentItem, instantiateItem, onPageSelected,
        // If showing position not changed, no trigger onPageSelected, so active it.
        if (lastPosition == curPosition) {
            // although position is not changed, but fragment(uri) maybe has changed, so check it.
            notifyPageChangedFromIndex(viewPager.getCurrentItem(), true, CHANGED_BY_SHOW);
        }
    }

    private void notifyPageChangedFromIndex(int position, boolean filter, int changeType) {
        IPageBean toBean = curInfoList.get(position);
        notifyPageChanged(toBean, changeType, filter);
    }

    @Override
    // It works as long as uri match success.
    public void showPage(@NonNull IPageBean bean) {
        int position;
        if ((position = curUriList.indexOf(bean.getPageUri())) != -1) {
            // if same with last, no trigger onPageSelected.
            changeByShow = true;
            // setCurrentItem is a sync method for onPageSelected.
            viewPager.setCurrentItem(position, false);
            changeByShow = false;
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {

        ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = newFragment(curUriList.get(position));
            Bundle info = null;
            if (curInfoList.get(position) != null && curInfoList.get(position).getPageInfo() != null) {
                info = curInfoList.get(position).getPageInfo();
            }
            putArgsForFragment(fragment, info);
            return fragment;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // Update difference uri (page), for the same position. We should remove it first.
            if (position < curUriList.size() && position < lastUriList.size()
                    && !curUriList.get(position).equals(lastUriList.get(position))) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                String name = makeFragmentName(container.getId(), position);
                Fragment fragment = fragmentManager.findFragmentByTag(name);
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

    private static String makeFragmentName(int viewId, long position) {
        return "android:switcher:" + viewId + ":" + position;
    }
}
