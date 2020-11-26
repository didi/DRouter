package com.didi.drouter.router;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gaowei on 2019/1/9
 */
class ResultAgent {

    // used inner, exists in Intent
    static final String FIELD_START_ACTIVITY_REQUEST_NUMBER = "DRouter_start_activity_request_number";

    static final String STATE_NOT_FOUND = "not_found";
    static final String STATE_TIMEOUT = "timeout";
    static final String STATE_ERROR = "error";
    static final String STATE_STOP_BY_INTERCEPTOR = "stop_by_interceptor";
    static final String STATE_STOP_BY_ROUTER_TARGET = "stop_by_router_target";
    static final String STATE_COMPLETE = "complete";
    static final String STATE_REQUEST_CANCEL = "request_cancel";

    // key is primary and branch number, add in constructor, to remove one by one
    private static final Map<String, Result> numberToResult = new ConcurrentHashMap<>();
    // key is branch number, add in constructor
    private final Map<String, Request> branchRequestMap = new ConcurrentHashMap<>();
    // key is branch number, value is reason, add one by one along with release
    private final Map<String, String> branchReasonMap = new ConcurrentHashMap<>();
    // primary
    @NonNull Request primaryRequest;
    private RouterCallback callback;

    // if only primary, branchRequests will only contains this primary request
    // branchRequests is null or size >= 1
    ResultAgent(@NonNull final Request primaryRequest, @Nullable Collection<Request> branchRequests,
                @NonNull final Result result,
                RouterCallback callback) {
        numberToResult.put(primaryRequest.getNumber(), result);
        this.primaryRequest = primaryRequest;
        this.callback = callback;
        if (branchRequests != null) {
            for (Request branch : branchRequests) {
                numberToResult.put(branch.getNumber(), result);
                branchRequestMap.put(branch.getNumber(), branch);
            }
        }
        if (primaryRequest.lifecycleOwner != null) {
            primaryRequest.lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    if (numberToResult.containsKey(primaryRequest.getNumber())) {
                        RouterLogger.getCoreLogger().w(
                                "request \"%s\" lifecycleOwner \"%s\" destroy and complete",
                                primaryRequest.getNumber(),
                                primaryRequest.lifecycleOwner.getLifecycle().getClass().getSimpleName());
                        // stop callback
                        ResultAgent.this.callback = null;
                        release(primaryRequest.getNumber(), STATE_REQUEST_CANCEL);
                    }
                }
            });
        }
    }

    @Nullable
    static Request getRequest(@Nullable String requestNumber) {
        Result result = getResult(requestNumber);
        return result != null ? result.agent.branchRequestMap.get(requestNumber) : null;
    }

    @Nullable
    static Result getResult(@Nullable String requestNumber) {
        if (TextUtils.isEmpty(requestNumber)) return null;
        return numberToResult.get(requestNumber);
    }

    static void release(Request request, String reason) {
        if (request != null) {
            release(request.getNumber(), reason);
        }
    }

    // primary or branch.
    private synchronized static void release(String requestNumber, String reason) {
        Result result = getResult(requestNumber);
        if (result != null) {
            if (result.agent.primaryRequest.getNumber().equals(requestNumber)) {
                // all clear
                if (result.agent.branchRequestMap.size() > 1) {
                    RouterLogger.getCoreLogger().w(
                            "be careful, all request \"%s\" will be cleared", requestNumber);
                }
                for (String number : result.agent.branchRequestMap.keySet()) {
                    if (!result.agent.branchReasonMap.containsKey(number)) {
                        completeBranch(number, reason);
                    }
                }
            } else {
                // branch only
                completeBranch(requestNumber, reason);
            }
            // check and release primary
            if (result.agent.branchReasonMap.size() == result.agent.branchRequestMap.size()) {
                completePrimary(result);
            }
        }
    }

    private synchronized static void completeBranch(String branchNumber, String reason) {
        Result result = numberToResult.get(branchNumber);
        if (result != null) {
            if (STATE_TIMEOUT.equals(reason)) {
                RouterLogger.getCoreLogger().w(
                        "request \"%s\" time out and force-complete", branchNumber);
            }
            result.agent.branchReasonMap.put(branchNumber, reason);
            numberToResult.remove(branchNumber);
            RouterLogger.getCoreLogger().d(
                    "==== request \"%s\" complete, reason \"%s\" ====", branchNumber, reason);
        }
    }

    // finish
    private synchronized static void completePrimary(@NonNull Result result) {
        RouterLogger.getCoreLogger().d("primary request \"%s\" complete, all reason %s",
                result.agent.primaryRequest.getNumber(), result.agent.branchReasonMap.toString());
        numberToResult.remove(result.agent.primaryRequest.getNumber());
        if (result.agent.callback != null) {
            result.agent.callback.onResult(result);
        }
        if (!numberToResult.containsKey(result.agent.primaryRequest.getNumber())) {
            RouterLogger.getCoreLogger().d(
                    "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        }
    }
}
