package com.didi.drouter.page;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.utils.RouterLogger;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by gaowei on 2020/4/1
 */
public abstract class RouterPageAbs implements IPageRouter {

    private final Set<IPageObserver> observers = new ArraySet<>();
    private IPageBean currentPage = new IPageBean.EmptyPageBean();
    // for stick
    private IPageBean lastPage = new IPageBean.EmptyPageBean();
    private int lastChangeType;

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
    public void addPageObserver(final IPageObserver listener, boolean sticky, @Nullable LifecycleOwner owner) {
        if (listener != null) {
            if (sticky && (!(lastPage instanceof IPageBean.EmptyPageBean) || !(currentPage instanceof IPageBean.EmptyPageBean))) {
                listener.onPageChange(lastPage, currentPage, lastChangeType);
            }
            observers.add(listener);
            if (owner != null) {
                final WeakReference<IPageObserver> reference = new WeakReference<>(listener);
                owner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        removePageObserver(reference.get());
                    }
                });
            }
        }
    }

    @Override
    public void removePageObserver(IPageObserver listener) {
        if (listener != null) {
            observers.remove(listener);
        }
    }

    /**
     * @param toUri target uri
     * @param changeType change reason {@link com.didi.drouter.page.IPageRouter.IPageObserver}
     * @param filter whether filter repeated page
     */
    protected void notifyPageChanged(IPageBean toUri, int changeType, boolean filter) {
        if (filter && toUri.getPageUri().equals(currentPage.getPageUri())) {
            return;
        }
        for (IPageObserver observer : observers) {
            observer.onPageChange(currentPage, toUri, changeType);
        }
        lastChangeType = changeType;
        lastPage = currentPage;
        currentPage = toUri;
    }

    protected @NonNull Fragment createFragment(String uri) {
        final Fragment[] fragments = {null};
        DRouter.build(uri).start(null, new RouterCallback() {
            @Override
            public void onResult(@NonNull Result result) {
                fragments[0] = result.getFragment();
            }
        });
        if (fragments[0] == null) {
            RouterLogger.getCoreLogger().e(
                    "PageRouter get null fragment with uri: \"%s\", StackTrace:\n %s",
                    uri, new Throwable());
            return new EmptyFragment();
        }
        return fragments[0];
    }

    protected void putArgsForFragment(Fragment fragment, Bundle... bundles) {
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
        @SuppressLint("SetTextI18n")
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            TextView textView = new TextView(getContext());
            textView.setText("RouterEmptyFragment");
            return textView;
        }
    }
}
