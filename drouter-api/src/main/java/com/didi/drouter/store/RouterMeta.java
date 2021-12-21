package com.didi.drouter.store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.didi.drouter.api.Extend;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.router.IRouterInterceptor;
import com.didi.drouter.router.RouterType;
import com.didi.drouter.service.IFeatureMatcher;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gaowei on 2018/8/30
 */
@SuppressWarnings("all")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RouterMeta {

    public static int ACTIVITY    = RouterType.ACTIVITY;
    public static int FRAGMENT    = RouterType.FRAGMENT;
    public static int VIEW        = RouterType.VIEW;
    public static int HANDLER     = RouterType.HANDLER;
    public static int INTERCEPTOR = RouterType.HANDLER + 1;
    public static int SERVICE     = RouterType.HANDLER + 2;
    private static String PLACE_HOLDER_REGEX = "<[a-zA-Z_]+\\w*>";  // no /
    private static Pattern sHolderPattern = Pattern.compile(PLACE_HOLDER_REGEX);

    private int routerType;
    private Class<?> routerClass;       // fragment, view, static handler, static service, interceptor
    private IRouterProxy routerProxy;   // fragment, view, static handler, static service, interceptor
    private int priority;               // router, interceptor, service
    private boolean isDynamic;

    // for router
    private @NonNull String scheme;
    private @NonNull String host;
    private @NonNull String path;    // If path is not "" or RegExp, must start with "/"
    private Boolean[] hasPlaceholder = new Boolean[3];
    private String activityName;
    private @Nullable Class<? extends IRouterInterceptor>[] interceptors;   //impl
    private @Nullable String[] interceptorNames;   //interceptor path
    private int thread;
    private boolean hold;
    private Intent intent;
    private IRouterHandler dynamicHandler;

    // for service
    private String serviceAlias;
    private @Nullable IFeatureMatcher<?> featureMatcher;   //instance
    private Object dynamicService;

    // for interceptor
    private boolean global;

    // for service and interceptor
    private int cache;

    private RouterMeta(int routerType) {
        this.routerType = routerType;
    }

    public static RouterMeta build(int routerType) {
        return new RouterMeta(routerType);
    }

    // key is uri, for activity
    public RouterMeta assembleRouter(String scheme, String host, String path,
                                     String routerClassName, IRouterProxy routerProxy,
                                     Class<? extends IRouterInterceptor>[] interceptors,
                                     String[] interceptorNames,
                                     int thread, int priority, boolean hold) {
        this.scheme = TextUtils.getNonNull(scheme);
        this.host = TextUtils.getNonNull(host);
        this.path = TextUtils.getNonNull(path);
        this.activityName = routerClassName;
        this.routerProxy = routerProxy;
        this.interceptors = interceptors;
        this.interceptorNames = interceptorNames;
        this.thread = thread;
        this.priority = priority;
        this.hold = hold;
        return this;
    }

    // key is uri, for fragment/view/handler
    public RouterMeta assembleRouter(String scheme, String host, String path,
                                     Class<?> routerClass, IRouterProxy routerProxy,
                                     Class<? extends IRouterInterceptor>[] interceptors,
                                     String[] interceptorNames,
                                     int thread, int priority, boolean hold) {
        this.scheme = TextUtils.getNonNull(scheme);
        this.host = TextUtils.getNonNull(host);
        this.path = TextUtils.getNonNull(path);
        this.routerClass = routerClass;
        this.routerProxy = routerProxy;
        this.interceptors = interceptors;
        this.interceptorNames = interceptorNames;
        this.thread = thread;
        this.priority = priority;
        this.hold = hold;
        return this;
    }

    public RouterMeta assembleRouter(Intent intent) {
        this.intent = intent;
        return this;
    }

    public void setDynamicHandler(@NonNull IRouterHandler handler) {
        this.dynamicHandler = handler;
        this.isDynamic = true;
    }

    // key is function
    public RouterMeta assembleService(Class<?> routerClass, IRouterProxy routerProxy,
                                      String alias, IFeatureMatcher<?> featureMatcher, int priority, int cache) {
        this.routerClass = routerClass;
        this.routerProxy = routerProxy;
        this.serviceAlias = alias;
        this.featureMatcher = featureMatcher;
        this.priority = priority;
        this.cache = cache;
        return this;
    }

    public void setDynamicService(Object dynamicService) {
        this.dynamicService = dynamicService;
        this.isDynamic = true;
    }

    // key is string name or impl
    public RouterMeta assembleInterceptor(Class<? extends IRouterInterceptor> routerClass, IRouterProxy routerProxy,
                                          int priority, boolean global, int cache) {
        this.routerClass = routerClass;
        this.routerProxy = routerProxy;
        this.priority = priority;
        this.global = global;
        this.cache = cache;
        return this;
    }

    public int getRouterType() {
        return routerType;
    }

    public String getActivityClassName() {
        return activityName;
    }

    public Class<?> getRouterClass() {
        return routerClass;
    }

    @Nullable
    public IRouterProxy getRouterProxy() {
        return routerProxy;
    }

    public String getSimpleClassName() {
        if (activityName != null) {
            return activityName.substring(activityName.lastIndexOf(".") + 1);
        } else if (routerClass != null) {
            return routerClass.getSimpleName();
        } else if (dynamicHandler != null) {
            return dynamicHandler.getClass().getName().substring(dynamicHandler.getClass().getName().lastIndexOf(".") + 1);
        } else {
            return null;
        }
    }

    // for router
    public Class<? extends IRouterInterceptor>[] getInterceptors() {
        return interceptors;
    }

    @Nullable
    public String[] getInterceptorNames() {
        return interceptorNames;
    }

    @Extend.Thread
    public int getThread() {
        return thread;
    }

    public boolean isHold() {
        return hold;
    }

    public Intent getIntent() {
        return intent;
    }

    // whether request uri match fuzzy meta,
    // when any of scheme host path in meta(@Router or RouterKey) contains regex or placeholder
    public boolean isRegexMatch(Uri uri) {
        String s = TextUtils.getNonNull(uri.getScheme());
        String h = TextUtils.getNonNull(uri.getHost());
        String p = TextUtils.getNonNull(uri.getPath());
        // placeholder to match, placeholder can not match part with /
        String schemeRegex = hasPlaceholder(0, scheme) ?
                scheme.replaceAll(PLACE_HOLDER_REGEX, "[^/]*") : scheme;
        String hostRegex = hasPlaceholder(1, host) ?
                host.replaceAll(PLACE_HOLDER_REGEX, "[^/]*") : host;
        String pathRegex = hasPlaceholder(2, path) ?
                path.replaceAll(PLACE_HOLDER_REGEX, "[^/]*") : path;
        return  s != null && s.matches(schemeRegex) &&
                h != null && h.matches(hostRegex) &&
                p != null && p.matches(pathRegex);
    }

    // whether meta contains regex or placeholder
    public boolean isRegexUri() {
        return TextUtils.isRegex(scheme) || TextUtils.isRegex(host) || TextUtils.isRegex(path);
    }

    private boolean hasPlaceholder(int index, String annotation) {
        if (hasPlaceholder[index] != null && hasPlaceholder[index] == false) {
            return false;
        }
        return hasPlaceholder[index] = sHolderPattern.matcher(annotation).find();
    }

    // no holder or inject success
    public boolean injectPlaceHolder(Uri uri, Bundle bundle) {
        return  injectOne(0, scheme, uri.getScheme(), bundle) &&
                injectOne(1, host, uri.getHost(), bundle) &&
                injectOne(2, path, uri.getPath(), bundle);
    }

    private boolean injectOne(int index, @NonNull String oriAnno, @Nullable String oriUri, Bundle bundle) {
        if (!hasPlaceholder(index, oriAnno) || oriUri == null) {
            return true;
        }
        Bundle b = new Bundle();
        // for reduce the boundary conditions, and no query part
        String restAnno = oriAnno;
        String restUri = oriUri;
        restAnno = "@@" + restAnno + "$$";
        restUri = "@@" + restUri + "$$";

        String key, value;
        // outside anno holder parts
        String[] annoSplits = restAnno.split(PLACE_HOLDER_REGEX);
        for (int i = 0; i < annoSplits.length; i++) {
            if (i + 1 < annoSplits.length) {
                String annoSplit = annoSplits[i];
                restAnno = restAnno.substring(annoSplit.length());
                if (!restUri.startsWith(annoSplit)) {
                    break;
                }
                restUri = restUri.substring(annoSplit.length());

                // for key
                Matcher matcher = sHolderPattern.matcher(restAnno);
                String holder = "";
                if (matcher.find()) {
                    holder = matcher.group();
                }
                key = holder.replace("<", "").replace(">", "");
                // for value
                String annoNextSplit = annoSplits[i + 1];
                int nextSplitStart = restUri.indexOf(annoNextSplit);
                value = restUri.substring(0, nextSplitStart);
                // store
                if (TextUtils.isEmpty(key)) {
                    break;
                }
                b.putString(key, value);

                restAnno = restAnno.substring(holder.length());
                restUri = restUri.substring(nextSplitStart);
            } else if (restUri.equals(restAnno)) {
                // check last part
                RouterLogger.getCoreLogger().d(
                        "inject <> success, annoPart=%s, uriPart=%s, result=%s",
                        oriAnno, oriUri, b);
                bundle.putAll(b);
                return true;
            }
        }
        RouterLogger.getCoreLogger().e(
                "inject place holder error, annoPart=%s, uriPart=%s", oriAnno, oriUri);
        return false;
    }

    public IRouterHandler getDynamicHandler() {
        return dynamicHandler;
    }

    // for service
    public String getServiceAlias() {
        return serviceAlias;
    }

    public int getCache() {
        return cache;
    }

    public Object getDynamicService() {
        return dynamicService;
    }

    @Nullable
    public IFeatureMatcher getFeatureMatcher() {
        return featureMatcher;
    }

    // for interceptor
    public boolean isGlobal() {
        return global;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

}
