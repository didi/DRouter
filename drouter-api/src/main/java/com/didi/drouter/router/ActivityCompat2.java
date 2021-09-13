package com.didi.drouter.router;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.didi.drouter.api.Extend;
import com.didi.drouter.utils.RouterLogger;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by gaowei on 2018/9/12
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ActivityCompat2 {

    private static final String TAG = "DRouterEmptyFragment";

    private static final AtomicInteger sCount = new AtomicInteger(0);
    private static final SparseArray<Pair<WeakReference<Activity>, RouterCallback.ActivityCallback>>
            sCallbackMap = new SparseArray<>();

    // start index, will not change when rotation or recycle
    private int cur;
    private final Active active;

    private ActivityCompat2(Active active) {
        this.active = active;
    }

    static void startActivityForResult(@NonNull final Activity activity,
                                       @NonNull final Intent intent, final int requestCode,
                                       RouterCallback.ActivityCallback callback) {
        final int cur = sCount.incrementAndGet();
        sCallbackMap.put(cur, new Pair<>(new WeakReference<>(activity), callback));
        final Active active;
        if (activity instanceof FragmentActivity) {
            active = new HolderFragmentV4();
        } else {
            active = new HolderFragment();
        }
        RouterLogger.getCoreLogger().d("HoldFragment start, put %s callback and page | isV4:",
                cur, active instanceof HolderFragmentV4);
        active.getCompat().cur = cur;
        active.attach(activity, intent, requestCode);
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            cur = savedInstanceState.getInt("cur");
        }
        active.startActivity();
//        RouterLogger.getCoreLogger().d("HoldFragment onCreate cur:" + cur);
    }

    private void onActivityResult(Activity activity, int resultCode, Intent data) {
        RouterCallback.ActivityCallback cb;
        Pair<WeakReference<Activity>, RouterCallback.ActivityCallback> pair = sCallbackMap.get(cur);
        if (pair != null && (cb = pair.second) != null) {
            RouterLogger.getCoreLogger().d("HoldFragment ActivityResult callback success");
            cb.onActivityResult(resultCode, data);
        }
        if (pair == null || pair.first == null || pair.first.get() != activity) {
            RouterLogger.getCoreLogger().e("HoldFragment onActivityResult warn, " +
                    "for host activity changed, but still callback last host");
        }
        RouterLogger.getCoreLogger().d("HoldFragment remove %s callback and page", cur);
        sCallbackMap.remove(cur);
        active.remove();
    }

    private void onSaveInstanceState(Bundle outState) {
        outState.putInt("cur", cur);
    }

    private void onDestroy() {
//        RouterLogger.getCoreLogger().d("HoldFragment onDestroy");
    }

    public static class HolderFragmentV4 extends Fragment implements Active {

        private final ActivityCompat2 activityCompat2;
        private Intent intent;
        private int requestCode;

        public HolderFragmentV4() {
            activityCompat2 = new ActivityCompat2(this);
        }

        @Override
        public void startActivity() {
            if (intent == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                startActivityForResult(intent, requestCode, intent.getBundleExtra(Extend.START_ACTIVITY_OPTIONS));
            } else {
                startActivityForResult(intent, requestCode);
            }
        }

        @Override
        public ActivityCompat2 getCompat() {
            return activityCompat2;
        }

        @Override
        public void attach(Activity activity, Intent intent, int requestCode) {
            this.intent = intent;
            this.requestCode = requestCode;
            FragmentManager fragmentManager = ((FragmentActivity)activity).getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(this, TAG);
            transaction.commit();
        }

        @Override
        public void remove() {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.remove(this);
                transaction.commit();
            }
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activityCompat2.onCreate(savedInstanceState);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            activityCompat2.onActivityResult(getActivity(), resultCode, data);
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);
            activityCompat2.onSaveInstanceState(outState);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            activityCompat2.onDestroy();
        }
    }

    @SuppressWarnings("deprecation")
    public static class HolderFragment extends android.app.Fragment implements Active {

        private final ActivityCompat2 activityCompat2;
        private Intent intent;
        private int requestCode;

        public HolderFragment() {
            activityCompat2 = new ActivityCompat2(this);
        }

        @Override
        public void startActivity() {
            if (intent == null) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                startActivityForResult(intent, requestCode, intent.getBundleExtra(Extend.START_ACTIVITY_OPTIONS));
            } else {
                startActivityForResult(intent, requestCode);
            }
        }

        @Override
        public ActivityCompat2 getCompat() {
            return activityCompat2;
        }

        @Override
        public void attach(Activity activity, Intent intent, int requestCode) {
            this.intent = intent;
            this.requestCode = requestCode;
            android.app.FragmentManager fragmentManager = activity.getFragmentManager();
            android.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(this, TAG);
            transaction.commit();
        }

        @Override
        public void remove() {
            android.app.FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                android.app.FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.remove(this);
                transaction.commit();
            }
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activityCompat2.onCreate(savedInstanceState);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            activityCompat2.onActivityResult(getActivity(), resultCode, data);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            activityCompat2.onSaveInstanceState(outState);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            activityCompat2.onDestroy();
        }
    }

    interface Active {
        ActivityCompat2 getCompat();
        void attach(Activity activity, Intent intent, int requestCode);
        void startActivity();
        void remove();
    }

}


