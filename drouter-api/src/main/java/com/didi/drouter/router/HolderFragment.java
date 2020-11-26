package com.didi.drouter.router;

import android.app.Activity;
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

import com.didi.drouter.api.Extend;
import com.didi.drouter.utils.RouterLogger;

import java.util.WeakHashMap;

/**
 * Created by gaowei on 2018/9/12
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class HolderFragment extends Fragment {

    private static final String TAG = "DRouterEmptyFragment";

    private boolean attached;
    private static final WeakHashMap<Activity, RouterCallback.ActivityCallback> callback = new WeakHashMap<>();

    public HolderFragment() {
        //RouterLogger.getCoreLogger().d("HoldFragment constructor");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //RouterLogger.getCoreLogger().d("HoldFragment onCreate");
        if (savedInstanceState != null) {
            attached = savedInstanceState.getBoolean("attached");
        }
    }

    public static void start(@NonNull FragmentActivity activity, @NonNull Intent intent, int requestCode,
                             RouterCallback.ActivityCallback callback) {
        HolderFragment holdFragment = new HolderFragment();
        HolderFragment.callback.put(activity, callback);

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(holdFragment, TAG);
        transaction.commit();
        RouterLogger.getCoreLogger().d("ActivityResult HoldFragment commit attach");
        fragmentManager.executePendingTransactions();

        //RouterLogger.getCoreLogger().d("HoldFragment startActivityForResult");
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
        if ((cb = callback.get(getActivity())) != null) {
            RouterLogger.getCoreLogger().d("ActivityResult callback");
            cb.onActivityResult(resultCode, data);
        } else {
            RouterLogger.getCoreLogger().d("ActivityResult callback fail for host activity destroyed");
        }
        callback.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        //RouterLogger.getCoreLogger().d("HoldFragment onResume");
        if (attached) {
            // second time
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(this);
            transaction.commit();
            attached = false;
            RouterLogger.getCoreLogger().d("ActivityResult HoldFragment commit remove");
        }
        attached = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("attached", attached);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //RouterLogger.getCoreLogger().d("HoldFragment onDestroy");
    }
}
