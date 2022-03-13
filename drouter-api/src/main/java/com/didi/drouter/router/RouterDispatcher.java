package com.didi.drouter.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.didi.drouter.api.Extend;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/5
 */
class RouterDispatcher {

    static void start(Request request, RouterMeta meta, Result result, RouterCallback callback) {

        RouterLogger.getCoreLogger().d("request \"%s\", class \"%s\" start execute",
                request.getNumber(),
                meta.getSimpleClassName());
        switch (meta.getRouterType()) {
            case RouterType.ACTIVITY:
                startActivity(request, meta, result, callback);
                break;
            case RouterType.FRAGMENT:
                startFragment(request, meta, result);
                break;
            case RouterType.VIEW:
                startView(request, meta, result);
                break;
            case RouterType.HANDLER:
                startHandler(request, meta, result, callback);
                break;
            default:
                break;
        }
    }

    private static void startActivity(Request request, RouterMeta meta, Result result, RouterCallback callback) {
        Context context = request.getContext();
        Intent intent = meta.getIntent();
        if (intent == null) {
            intent = new Intent();
            Class<?> routerClass = meta.getRouterClass();
            // 如果有class优先使用class
            if (routerClass != null) {
                intent.setClass(context, routerClass);
            } else {
                intent.setClassName(context, meta.getActivityClassName());
            }
        }
        if (request.getExtra().containsKey(Extend.START_ACTIVITY_FLAGS)) {
            intent.setFlags(request.getInt(Extend.START_ACTIVITY_FLAGS));
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(ResultAgent.FIELD_START_ACTIVITY_REQUEST_NUMBER, request.getNumber());
        intent.putExtras(request.getExtra());
        boolean hasRequestCode = request.getExtra().containsKey(Extend.START_ACTIVITY_REQUEST_CODE);
        int requestCode = hasRequestCode? request.getInt(Extend.START_ACTIVITY_REQUEST_CODE) : 1024;
        if (context instanceof Activity && callback instanceof RouterCallback.ActivityCallback) {
            ActivityCompat2.startActivityForResult((Activity) context, intent,
                    requestCode, (RouterCallback.ActivityCallback) callback);
        } else if (context instanceof Activity && hasRequestCode) {
            ActivityCompat.startActivityForResult((Activity) context, intent,
                    requestCode, intent.getBundleExtra(Extend.START_ACTIVITY_OPTIONS));
        } else {
            ActivityCompat.startActivity(context, intent, intent.getBundleExtra(Extend.START_ACTIVITY_OPTIONS));
        }
        int[] anim = request.getIntArray(Extend.START_ACTIVITY_ANIMATION);
        if (context instanceof Activity && anim != null && anim.length == 2) {
            ((Activity) context).overridePendingTransition(anim[0], anim[1]);
        }
        result.isActivityStarted = true;
        if (meta.isHold() && callback != null) {
            RouterLogger.getCoreLogger().w("request \"%s\" will be hold", request.getNumber());
            Monitor.startMonitor(request, result);
        } else {
            ResultAgent.release(request, ResultAgent.STATE_COMPLETE);
        }
    }

    private static void startFragment(Request request, RouterMeta meta, Result result) {
        result.routerClass = meta.getRouterClass();
        if (request.getExtra().getBoolean(Extend.START_FRAGMENT_NEW_INSTANCE, true)) {
            Object object = meta.getRouterProxy() != null ?
                    meta.getRouterProxy().newInstance(null) : null;
            if (object instanceof Fragment) {
                result.fragment = (Fragment) object;
                result.fragment.setArguments(request.getExtra());
            }
        }
        ResultAgent.release(request, ResultAgent.STATE_COMPLETE);
    }

    private static void startView(Request request, RouterMeta meta, Result result) {
        result.routerClass = meta.getRouterClass();
        if (request.getExtra().getBoolean(Extend.START_VIEW_NEW_INSTANCE, true)) {
            Object object = meta.getRouterProxy() != null ?
                    meta.getRouterProxy().newInstance(request.getContext()) : null;
            if (object instanceof View) {
                result.view = (View) object;
                result.view.setTag(request.getExtra());
            }
        }
        ResultAgent.release(request, ResultAgent.STATE_COMPLETE);
    }

    private static void startHandler(final Request request, final RouterMeta meta,
                                     final Result result, final RouterCallback callback) {
        // dynamic
        IRouterHandler handler = meta.getDynamicHandler();
        if (handler == null) {
            handler = meta.getRouterProxy() != null ?
                    (IRouterHandler) meta.getRouterProxy().newInstance(null) : null;
        }
        if (handler != null) {
            final IRouterHandler finalHandler = handler;
            RouterExecutor.execute(meta.getThread(), () -> {
                if (meta.isHold()) {
                    RouterLogger.getCoreLogger().w("request \"%s\" will hold", request.getNumber());
                }
                finalHandler.handle(request, result);
                if (meta.isHold() && callback != null) {
                    Monitor.startMonitor(request, result);
                } else {
                    ResultAgent.release(request, ResultAgent.STATE_COMPLETE);
                }
            });
        } else {
            ResultAgent.release(request, ResultAgent.STATE_ERROR);
        }


    }

}
