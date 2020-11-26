package com.didi.drouter.router;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.Collection;

/**
 * Created by gaowei on 2019/1/9
 */
public class Result extends DataExtras<Result> {

    ResultAgent agent;

    Class<?> routerClass;
    boolean isActivityStarted;
    Fragment fragment;
    View view;
    private final int routerSize;

    Result(@NonNull Request primaryRequest,
           @Nullable Collection<Request> branchRequests,
           RouterCallback callback) {
        agent = new ResultAgent(primaryRequest, branchRequests, this, callback);
        routerSize = branchRequests != null ? branchRequests.size() : 0;
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
