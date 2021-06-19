package com.didi.drouter.store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

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
    private static String PLACE_HOLDER_REGEX = "<[a-zA-Z_]+\\w*>";
    private static Pattern pattern = Pattern.compile(PLACE_HOLDER_REGEX);

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
    private RouterKey routerKey;
    private IRouterHandler handler;

    // for service
    private String serviceAlias;
    private @Nullable IFeatureMatcher<?> featureMatcher;   //instance
    private ServiceKey serviceKey;
    private Object service;

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
        this.scheme = scheme;
        this.host = host;
        this.path = path;
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
        this.scheme = scheme;
        this.host = host;
        this.path = path;
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

    // for dynamic handler
    public void setHandler(RouterKey key, @NonNull IRouterHandler handler) {
        this.routerKey = key;
        this.handler = handler;
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

    // for dynamic service
    public void setService(ServiceKey key, Object service) {
        this.serviceKey = key;
        this.service = service;
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
        } else if (handler != null) {
            return handler.getClass().getName().substring(handler.getClass().getName().lastIndexOf(".") + 1);
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

    public int getThread() {
        return thread;
    }

    public boolean isHold() {
        return hold;
    }

    public Intent getIntent() {
        return intent;
    }

    // check whether it matches
    // when any of scheme host path in @Router or RouterKey is regex or placeholder
    public boolean isRegexMatch(Uri uri) {
        String s = uri.getScheme();
        String h = uri.getHost();
        String p = uri.getPath();
        // placeholder to match
        String schemeRegex = hasPlaceholder(0, scheme) ?
                scheme.replaceAll(PLACE_HOLDER_REGEX, ".*") : scheme;
        String hostRegex = hasPlaceholder(1, host) ?
                host.replaceAll(PLACE_HOLDER_REGEX, ".*") : host;
        String pathRegex = hasPlaceholder(2, path) ?
                path.replaceAll(PLACE_HOLDER_REGEX, ".*") : path;
        return  s != null && s.matches(schemeRegex) &&
                h != null && h.matches(hostRegex) &&
                p != null && p.matches(pathRegex);
    }

    // check whether router key is regex or placeholder
    public boolean isRegexUri() {
        return TextUtils.isRegex(scheme) || TextUtils.isRegex(host) || TextUtils.isRegex(path);
    }

    private boolean hasPlaceholder(int index, String annotation) {
        if (hasPlaceholder[index] != null && hasPlaceholder[index] == false) {
            return false;
        }
        return hasPlaceholder[index] = pattern.matcher(annotation).find();
    }

    // no holder or inject success
    public boolean injectPlaceHolder(Uri uri, Bundle bundle) {
        return  analyseOne(0, scheme, uri.getScheme(), bundle) &&
                analyseOne(1, host, uri.getHost(), bundle) &&
                analyseOne(2, path, uri.getPath(), bundle);
    }

    private boolean analyseOne(int index, @NonNull String oriAnnoPart, @Nullable String oriUriPart, Bundle bundle) {
        if (!hasPlaceholder(index, oriAnnoPart) || oriUriPart == null) {
            return true;
        }
        Bundle b = new Bundle();
        String annoPart = oriAnnoPart;
        String uriPart = oriUriPart;
        annoPart = "&&" + annoPart + "$$";
        uriPart = "&&" + uriPart + "$$";

        String key, value;
        String[] splits = annoPart.split(PLACE_HOLDER_REGEX);

        for (int i = 0; i < splits.length; i++) {
            if (i + 1 < splits.length) {
                String annoSplit = splits[i];
                annoPart = annoPart.substring(annoSplit.length());
                if (!uriPart.startsWith(annoSplit)) {
                    break;
                }
                uriPart = uriPart.substring(annoSplit.length());

                Matcher matcher = pattern.matcher(annoPart);
                String holder = "";
                if (matcher.find()) {
                    holder = matcher.group();
                }
                key = holder.replace("<", "").replace(">", "");

                String annoNextSplit = splits[i + 1];
                int nextSplitStart = uriPart.indexOf(annoNextSplit);
                value = uriPart.substring(0, nextSplitStart);

                if (TextUtils.isEmpty(key)) {
                    break;
                }
                b.putString(key, value);

                annoPart = annoPart.substring(holder.length());
                uriPart = uriPart.substring(nextSplitStart);
            } else if (uriPart.equals(annoPart)) {
                RouterLogger.getCoreLogger().d(
                        "inject <> success, annoPart=%s, uriPart=%s, result=%s",
                        oriAnnoPart, oriUriPart, b);
                bundle.putAll(b);
                return true;
            }
        }
        RouterLogger.getCoreLogger().e(
                "inject place holder error, annoPart=%s, uriPart=%s", oriAnnoPart, oriUriPart);
        return false;
    }

    public String getLegalUri() {
        return scheme + "://" + host + path;
    }

    public IRouterHandler getHandler() {
        return handler;
    }

    // for service
    public String getServiceAlias() {
        return serviceAlias;
    }

    public int getCache() {
        return cache;
    }

    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    public Object getService() {
        return service;
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
