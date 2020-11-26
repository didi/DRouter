package com.didi.drouter.router;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import android.util.SparseArray;

import com.didi.drouter.api.Extend;
import com.didi.drouter.remote.RemoteBridge;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by gaowei on 2018/6/22
 */
class RouterLoader {

    private Request primaryRequest;
    private RouterCallback callback;

    private RouterLoader() {}

    @NonNull
    static RouterLoader build(Request request, RouterCallback callback) {
        RouterLoader loader = new RouterLoader();
        loader.primaryRequest = request;
        loader.callback = callback;
        return loader;
    }

    void start() {
        RouterLogger.getCoreLogger().d(
                "---------------------------------------------------------------------------");
        RouterLogger.getCoreLogger().d("primary request \"%s\", router uri \"%s\", need callback \"%s\"",
                primaryRequest.getNumber(), primaryRequest.getUri(), callback != null);
        if (TextUtils.isEmpty(primaryRequest.authority)) {
            startLocal();
        } else {
            startRemote();
        }
    }

    private void startLocal() {
        TextUtils.appendExtra(primaryRequest.getExtra(), TextUtils.getQuery(primaryRequest.getUri()));
        Map<Request, RouterMeta> requestMap = makeRequest();

        if (requestMap.isEmpty()) {
            RouterLogger.getCoreLogger().w("warning: there is no request target match");
            new Result(primaryRequest, null, callback);
            ResultAgent.release(primaryRequest, ResultAgent.STATE_NOT_FOUND);
            return;
        }

        final Result result = new Result(primaryRequest, requestMap.keySet(), callback);
        if (requestMap.size() > 1) {
            RouterLogger.getCoreLogger().w("warning: request match %s targets", requestMap.size());
        }
        List<Map.Entry<Request, RouterMeta>> requestList = new ArrayList<>(requestMap.entrySet());
        Collections.sort(requestList, new RouterComparator());
        final boolean[] stopByRouterTarget = {false};
        for (final Map.Entry<Request, RouterMeta> entry : requestList) {
            if (stopByRouterTarget[0]) {
                // one by one
                ResultAgent.release(entry.getKey(), ResultAgent.STATE_STOP_BY_ROUTER_TARGET);
                continue;
            }
            InterceptorHandler.handle(entry.getKey(), entry.getValue(), new IRouterInterceptor.IInterceptor() {
                @Override
                public void onContinue() {
                    entry.getKey().interceptor = new IRouterInterceptor.IInterceptor() {
                        @Override
                        public void onContinue() {
                        }

                        @Override
                        public void onInterrupt() {
                            RouterLogger.getCoreLogger().w(
                                    "request \"%s\" stop all remains requests", entry.getKey().getNumber());
                            stopByRouterTarget[0] = true;
                        }
                    };
                    RouterDispatcher.start(entry.getKey(), entry.getValue(), result, callback);
                    entry.getKey().interceptor = null;
                }

                @Override
                public void onInterrupt() {
                    ResultAgent.release(entry.getKey(), ResultAgent.STATE_STOP_BY_INTERCEPTOR);
                }
            });
        }
    }

    @NonNull
    private Map<Request, RouterMeta> makeRequest() {
        Map<Request, RouterMeta> requestMap = new HashMap<>();
        Parcelable parcelable = primaryRequest.getParcelable(Extend.START_ACTIVITY_VIA_INTENT);
        if (parcelable instanceof Intent) {
            primaryRequest.getExtra().remove(Extend.START_ACTIVITY_VIA_INTENT);
            Intent intent = (Intent) parcelable;
            RouterLogger.getCoreLogger().d("request %s, intent \"%s\"", primaryRequest.getNumber(), intent);
            PackageManager pm = primaryRequest.getContext().getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (!activities.isEmpty()) {
                primaryRequest.routerType = RouterType.ACTIVITY;
                RouterLogger.getCoreLogger().d("request \"%s\" find target class \"%s\", type \"%s\"",
                        primaryRequest.getNumber(), activities.get(0).activityInfo.name, primaryRequest.routerType);
                requestMap.put(this.primaryRequest, RouterMeta.build(RouterType.ACTIVITY).assembleRouter(intent));
            }
        } else {
            Set<RouterMeta> metas = getAllRouterMetas();
            int index = 0;
            for (RouterMeta routerMeta : metas) {
                Request request = createBranchRequest(
                        this.primaryRequest, metas.size() > 1, routerMeta.getRouterType(), index++);
                RouterLogger.getCoreLogger().d(
                        "request \"%s\" find target class \"%s\", type \"%s\", priority \"%s\"",
                        request.getNumber(), routerMeta.getSimpleClassName(),
                        routerMeta.getRouterType(), routerMeta.getPriority());
                requestMap.put(request, routerMeta);
            }
        }
        return requestMap;
    }

    @NonNull
    private Set<RouterMeta> getAllRouterMetas() {
        Set<RouterMeta> matchMetas = RouterStore.getRouterMetas(TextUtils.getUriKey(primaryRequest.getUri()));
        String schemeHost = primaryRequest.getString(Extend.START_ACTIVITY_WITH_DEFAULT_SCHEME_HOST);
        if (!TextUtils.isEmpty(schemeHost) && primaryRequest.getUri().toString().startsWith(schemeHost.toLowerCase())) {
            Set<RouterMeta> degradeMetas =
                    RouterStore.getRouterMetas(TextUtils.getUriKey(primaryRequest.getUri().getPath()));
            for (RouterMeta meta : degradeMetas) {
                if (meta.getRouterType() == RouterType.ACTIVITY) {
                    matchMetas.add(meta);
                }
            }
        }
        SparseArray<RouterMeta> sparseArray = new SparseArray<>();
        Set<RouterMeta> output = new ArraySet<>();
        for (RouterMeta meta : matchMetas) {
            if (meta.getRouterType() == RouterType.ACTIVITY) {
                if (sparseArray.get(0) != null) {
                    RouterLogger.getCoreLogger().w(
                            "warning: request match more than one activity and this \"%s\" will be ignored",
                            meta.getSimpleClassName());
                } else {
                    sparseArray.put(0, meta);
                }
            } else if (meta.getRouterType() == RouterType.FRAGMENT) {
                if (sparseArray.get(1) != null) {
                    RouterLogger.getCoreLogger().w(
                            "warning: request match more than one fragment and this \"%s\" will be ignored",
                            meta.getSimpleClassName());
                } else {
                    sparseArray.put(1, meta);
                }
            } else if (meta.getRouterType() == RouterType.VIEW) {
                if (sparseArray.get(2) != null) {
                    RouterLogger.getCoreLogger().w(
                            "warning: request match more than one view and this \"%s\" will be ignored",
                            meta.getSimpleClassName());
                } else {
                    sparseArray.put(2, meta);
                }
            } else if (meta.getRouterType() == RouterType.HANDLER) {
                output.add(meta);
            }
        }
        // only one activity/fragment/view
        if (sparseArray.get(0) != null) {
            output.add(sparseArray.get(0));
        } else if (sparseArray.get(1) != null) {
            output.add(sparseArray.get(1));
        } else if (sparseArray.get(2) != null) {
            output.add(sparseArray.get(2));
        }
        return output;
    }

    private void startRemote() {
        Result result = new Result(primaryRequest, Collections.singleton(primaryRequest), callback);
        RemoteBridge.load(primaryRequest.authority, primaryRequest.resendStrategy,
                primaryRequest.lifecycleOwner != null ? new WeakReference<>(primaryRequest.lifecycleOwner) : null)
                .start(primaryRequest, result, callback);
    }

    private static Request createBranchRequest(Request primaryRequest, boolean isBranch,
                                               @RouterType int type, int branchIndex) {
        primaryRequest.routerType = isBranch ? RouterType.MULTIPLE : type;
        if (isBranch) {
            Request branchRequest = Request.build(primaryRequest.getUri().toString());
            branchRequest.extra = primaryRequest.extra;
            branchRequest.addition = primaryRequest.addition;
            branchRequest.context = primaryRequest.context;
            branchRequest.lifecycleOwner = primaryRequest.lifecycleOwner;
            branchRequest.authority = primaryRequest.authority;
            branchRequest.resendStrategy = primaryRequest.resendStrategy;
            branchRequest.holdTimeout = primaryRequest.holdTimeout;
            branchRequest.serialNumber = primaryRequest.getNumber() + "_" + branchIndex;
            branchRequest.routerType = type;
            return branchRequest;
        }
        return primaryRequest;
    }

    // from large to small, static
    private static class RouterComparator implements Comparator<Map.Entry<Request, RouterMeta>> {
        @Override
        public int compare(Map.Entry<Request, RouterMeta> o1, Map.Entry<Request, RouterMeta> o2) {
            return o2.getValue().getPriority() - o1.getValue().getPriority();
        }
    }

}
