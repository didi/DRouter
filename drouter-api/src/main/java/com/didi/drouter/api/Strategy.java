package com.didi.drouter.api;

import androidx.annotation.IntDef;

import com.didi.drouter.utils.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by gaowei on 2022/01/25
 * IPC strategy
 * api 'io.github.didi:drouter-api-process:xxx' is needed.
 */
public class Strategy {

    public String authority;
    public boolean callAsync;
    public @Resend int resend;

    public Strategy(String authority) {
        if (TextUtils.isEmpty(authority)) throw new RuntimeException("remote authority is empty");
        this.authority = authority;
    }

    /**
     * If set true, use oneway aidl and then you can't get the result value from remote process.
     * @param async is call async.
     */
    public Strategy setCallAsync(boolean async) {
        this.callAsync = async;
        return this;
    }

    /**
     * If set, it will auto stop resend behavior when lifecycle is destroy.
     * It will take effect for all execute by this build,
     * for example, if this owner is destroyed, all the execute command resend will be stopped.
     */
    public Strategy setResend(@Resend int resend) {
        this.resend = resend;
        return this;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Resend.NO_RESEND, Resend.WAIT_ALIVE})
    public @interface Resend {
        /**
         * Resend strategy: No resend
         */
        int NO_RESEND   = 0;
        /**
         * Resend strategy: Client command will retain and wait until Server restart
         * Please refer to {@link RouterLifecycle} to control resend switch
         */
        int WAIT_ALIVE  = 1;
    }
}
