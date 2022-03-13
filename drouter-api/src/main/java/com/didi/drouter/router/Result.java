package com.didi.drouter.router;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Collection;

/**
 * Created by gaowei on 2019/1/9
 *
 * Multi request branch -> One result
 */
public class Result extends DataExtras<Result> {

    ResultAgent agent;

    Class<?> routerClass;
    boolean isActivityStarted;
    Fragment fragment;
    View view;
    int routerSize;

    Result(@NonNull Request primaryRequest,
           @NonNull Collection<Request> branchRequests,
           int size,
           RouterCallback callback) {
        agent = new ResultAgent(primaryRequest, branchRequests, this, callback);
        routerSize = size;
    }

    public @NonNull Request getRequest() {
        return agent.primaryRequest;
    }

    // fragment or view
    public Class<?> getRouterClass() {
        return routerClass;
    }

    public boolean isActivityStarted() {
        return isActivityStarted;
    }

    public Fragment getFragment() {
        return fragment;
    }

    public View getView() {
        return view;
    }

    // router count
    public int getRouterSize() {
        return routerSize;
    }

}
