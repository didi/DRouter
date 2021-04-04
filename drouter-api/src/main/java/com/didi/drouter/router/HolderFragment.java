package com.didi.drouter.router;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Pair;
import android.util.SparseArray;

import com.didi.drouter.api.Extend;
import com.didi.drouter.utils.RouterLogger;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaowei on 2018/9/12
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class HolderFragment extends Fragment {

    private static final String TAG = "DRouterEmptyFragment";

    private boolean attached;
    private int cur;
    private static AtomicInteger sCount = new AtomicInteger(0);
    private static SparseArray<Pair<WeakReference<FragmentActivity>, RouterCallback.ActivityCallback>>
            sCallbackMap = new SparseArray<>();

    public HolderFragment() {
        //RouterLogger.getCoreLogger().d("HoldFragment constructor");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            attached = savedInstanceState.getBoolean("attached");
            cur = savedInstanceState.getInt("cur");
        }
        //RouterLogger.getCoreLogger().d("HoldFragment onCreate cur:" + cur);
    }

    public static void start(@NonNull FragmentActivity activity, @NonNull Intent intent, int requestCode,
                             RouterCallback.ActivityCallback callback) {
        HolderFragment holdFragment = new HolderFragment();
        holdFragment.cur = sCount.incrementAndGet();
        sCallbackMap.put(holdFragment.cur, new Pair<>(new WeakReference<>(activity), callback));
        //RouterLogger.getCoreLogger().d("HoldFragment start, sCallbackMap.put:" + holdFragment.cur);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(holdFragment, TAG);
        transaction.commit();
        //RouterLogger.getCoreLogger().d("HoldFragment commit attach");
        fragmentManager.executePendingTransactions();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holdFragment.startActivityForResult(intent, requestCode,
                    intent.getBundleExtra(Extend.START_ACTIVITY_OPTIONS));
        } else {
            holdFragment.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        RouterCallback.ActivityCallback cb;
        Pair<WeakReference<FragmentActivity>, RouterCallback.ActivityCallback> pair = sCallbackMap.get(cur);
        if (pair != null && (cb = pair.second) != null) {
            RouterLogger.getCoreLogger().d("HoldFragment ActivityResult callback success");
            cb.onActivityResult(resultCode, data);
        }
        if (pair == null || pair.first == null || pair.first.get() != getActivity()) {
            RouterLogger.getCoreLogger().e("HoldFragment onActivityResult warn, " +
                    "for host activity changed, but still callback last host");
        }
        sCallbackMap.remove(cur);
//        RouterLogger.getCoreLogger().d("HoldFragment sCallbackMap.remove:" + cur);
    }

    @Override
    public void onResume() {
        super.onResume();
        //RouterLogger.getCoreLogger().d("HoldFragment onResume");
        if (attached) {
            // 2. back to front again, used to remove this hold fragment
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(this);
            transaction.commit();
            attached = false;
            //RouterLogger.getCoreLogger().d("HoldFragment commit remove");
        }
        // 1. set tag
        attached = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("attached", attached);
        outState.putInt("cur", cur);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //RouterLogger.getCoreLogger().d("HoldFragment onDestroy");
    }
}
