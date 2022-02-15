package com.didi.drouter.api;

import androidx.annotation.IntDef;

import com.didi.drouter.router.RouterCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by gaowei on 2018/9/13
 */
public class Extend {

    /**
     * value:String, this represent uri when building, used for getting primary uri from Activity/Fragment/View
     */
    public static final String REQUEST_BUILD_URI = "router_request_build_uri";

    /**
     * value:Bundle, optionsBundle for Activity.
     */
    public static final String START_ACTIVITY_OPTIONS = "router_start_activity_options";

    /**
     * value:int[], animation for Activity.
     */
    public static final String START_ACTIVITY_ANIMATION = "router_start_activity_animation";

    /**
     * value:int, flags for Activity.
     */
    public static final String START_ACTIVITY_FLAGS = "router_start_activity_flags";

    /**
     * value:Intent, use intent to start Activity, this will ignore uri in build at the same time.
     */
    public static final String START_ACTIVITY_VIA_INTENT = "router_start_activity_via_intent";

    /**
     * value:int, assign RequestCode for startActivityForResult.
     * {@link RouterCallback.ActivityCallback}
     */
    public static final String START_ACTIVITY_REQUEST_CODE = "router_start_activity_request_code";

    /**
     * value:Boolean，Used for Fragment, whether create fragment instance, default true.
     */
    public static final String START_FRAGMENT_NEW_INSTANCE = "router_start_fragment_new_instance";

    /**
     * value:Boolean，Used for View, whether create fragment instance, default true.
     */
    public static final String START_VIEW_NEW_INSTANCE = "router_start_view_new_instance";

    /**
     * value:String, format "scheme://host", used for Activity.
     * When your Activity has path only, you can use this to compatible with those Activity.
     * All those Activity will auto add scheme://host prefix for only this request to take a match.
     */
    public static final String
            START_ACTIVITY_WITH_DEFAULT_SCHEME_HOST = "router_start_activity_with_default_scheme_host";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Thread.POSTING, Thread.MAIN, Thread.WORKER})
    public @interface Thread {
        /**
         * No thread switch
         */
        int POSTING     = 0;
        /**
         * UI thread
         */
        int MAIN        = 1;
        /**
         * Worker thread
         */
        int WORKER      = 2;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Cache.NO, Cache.WEAK, Cache.SINGLETON})
    public @interface Cache {
        int NO          = 0;
        int WEAK        = 1;
        int SINGLETON   = 2;
    }
}
