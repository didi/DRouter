package com.didi.drouter.router;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.didi.drouter.utils.RouterExecutor;
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
    static final String FIELD_START_ACTIVITY_REQUEST_NUMBER = "router_start_activity_request_number";

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
    private final RouterCallback callback;

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
            RouterExecutor.main(new Runnable() {
                @Override
                public void run() {
                    primaryRequest.lifecycleOwner.getLifecycle().addObserver(observer);
                }
            });
        }
    }

    private final LifecycleObserver observer = new LifecycleEventObserver() {
        @Override
        public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                if (numberToResult.containsKey(primaryRequest.getNumber())) {
                    RouterLogger.getCoreLogger().w(
                            "request \"%s\" lifecycleOwner \"%s\" destroy and complete",
                            primaryRequest.getNumber(),
                            primaryRequest.lifecycleOwner.getLifecycle().getClass().getSimpleName());
                    release(primaryRequest.getNumber(), STATE_REQUEST_CANCEL);
                }
            }
        }
    };

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
    private synchronized static void completePrimary(@NonNull final Result result) {
        RouterLogger.getCoreLogger().d(
                "primary request \"%s\" complete, router uri \"%s\", all reason %s",
                result.agent.primaryRequest.getNumber(), result.agent.primaryRequest.getUri(),
                result.agent.branchReasonMap.toString());
        numberToResult.remove(result.agent.primaryRequest.getNumber());
        if (result.agent.callback != null) {
            result.agent.callback.onResult(result);
        }
        if (result.agent.primaryRequest.lifecycleOwner != null) {
            RouterExecutor.main(new Runnable() {
                @Override
                public void run() {
                    result.agent.primaryRequest.lifecycleOwner.getLifecycle().removeObserver(result.agent.observer);
                }
            });
        }
        if (!numberToResult.containsKey(result.agent.primaryRequest.getNumber())) {
            RouterLogger.getCoreLogger().d(
                    "Request finish " +
                            "------------------------------------------------------------");
        }
    }
}
