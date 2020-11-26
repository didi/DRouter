package com.didi.drouter.api;

import android.support.annotation.IntDef;

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
    public static final String REQUEST_BUILD_URI = "DRouter_request_build_uri";

    /**
     * value:Bundle, optionsBundle for Activity.
     */
    public static final String START_ACTIVITY_OPTIONS = "DRouter_start_activity_options";

    /**
     * value:int[], animation for Activity.
     */
    public static final String START_ACTIVITY_ANIMATION = "DRouter_start_activity_animation";

    /**
     * value:int, flags for Activity.
     */
    public static final String START_ACTIVITY_FLAGS = "DRouter_start_activity_flags";

    /**
     * value:Intent, use intent to start Activity, this will ignore uri in build at the same time.
     */
    public static final String START_ACTIVITY_VIA_INTENT = "DRouter_start_activity_via_intent";

    /**
     * value:int, assign RequestCode for startActivityForResult.
     * {@link RouterCallback.ActivityCallback}
     */
    public static final String START_ACTIVITY_REQUEST_CODE = "DRouter_start_activity_request_code";

    /**
     * value:Boolean，Used for Fragment, whether create fragment instance, default true.
     */
    public static final String START_FRAGMENT_NEW_INSTANCE = "DRouter_start_fragment_new_instance";

    /**
     * value:Boolean，Used for View, whether create fragment instance, default true.
     */
    public static final String START_VIEW_NEW_INSTANCE = "DRouter_start_view_new_instance";

    /**
     * value:String, format "scheme://host", used for Activity.
     * When your Activity has path only, you can use this to compatible with those Activity.
     * All those Activity will auto add scheme://host prefix for only this request to take a match.
     */
    public static final String
            START_ACTIVITY_WITH_DEFAULT_SCHEME_HOST = "DRouter_start_activity_with_default_scheme_host";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Resend.NO_RESEND, Resend.WAIT_ALIVE})
    public @interface Resend {
        int NO_RESEND   = 0;
        int WAIT_ALIVE  = 1;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Thread.POSTING, Thread.MAIN, Thread.WORKER})
    public @interface Thread {
        int POSTING     = 0;
        int MAIN        = 1;
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
