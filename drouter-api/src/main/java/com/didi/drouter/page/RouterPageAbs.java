package com.didi.drouter.page;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.didi.drouter.R;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.utils.RouterLogger;

import java.util.Set;

/**
 * Created by gaowei on 2020/4/1
 */
public abstract class RouterPageAbs implements IPageRouter {

    protected Set<IPageObserver> observers = new ArraySet<>();
    protected IPageBean currentPage = new IPageBean.EmptyPageBean();
    protected Bundle bundle = new Bundle();

    @Override
    public void popPage() {

    }

    @Override
    public @NonNull IPageBean getCurPage() {
        return currentPage;
    }

    @Override
    public Bundle execute(String method, Bundle bundle) {
        return null;
    }

    @Override
    public void addPageObserver(final IPageObserver listener, @Nullable LifecycleOwner owner) {
        observers.add(listener);
        if (owner != null) {
            owner.getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    removePageObserver(listener);
                }
            });
        }
    }

    @Override
    public void removePageObserver(IPageObserver listener) {
        observers.remove(listener);
    }

    protected void notifyPageChanged(IPageBean toUri) {
        if (!toUri.getPageUri().equals(currentPage.getPageUri())) {
            for (IPageObserver observer : observers) {
                observer.onPageChange(currentPage, toUri);
            }
            currentPage = toUri;
        }
    }

    protected @NonNull Fragment newFragment(String uri) {
        final Fragment[] fragments = {null};
        DRouter.build(uri).start(null, new RouterCallback() {
            @Override
            public void onResult(@NonNull Result result) {
                fragments[0] = result.getFragment();
            }
        });
        if (fragments[0] == null) {
            RouterLogger.getCoreLogger().e(
                    "PageRouter get null fragment with uri: \"%s\", StackTrace:\n %s", uri, new Throwable());
            return new EmptyFragment();
        }
        return fragments[0];
    }

    protected void addArgsForFragment(Fragment fragment, Bundle... bundles) {
        Bundle bundle = new Bundle();
        if (fragment.getArguments() != null) {
            bundle.putAll(fragment.getArguments());
        }
        for (Bundle arg : bundles) {
            if (arg != null) {
                bundle.putAll(arg);
            }
        }
        fragment.setArguments(bundle);
    }

    public static class EmptyFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            TextView textView = new TextView(getContext());
            textView.setText(R.string.drouter_empty_fragment);
            return textView;
        }
    }
}
