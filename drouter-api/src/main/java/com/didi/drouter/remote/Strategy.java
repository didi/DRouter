package com.didi.drouter.remote;

import com.didi.drouter.api.Extend;
import com.didi.drouter.utils.TextUtils;

/**
 * Created by gaowei on 2022/01/25
 */
public class Strategy {

    String authority;
    boolean callAsync;
    @Extend.Resend int resend;

    public Strategy(String authority) {
        if (TextUtils.isEmpty(authority)) throw new RuntimeException("remote authority is empty");
        this.authority = authority;
    }

    /**
     * If set, it will auto stop resend behavior when lifecycle is destroy.
     * It will take effect for all execute by this build,
     * for example, if this owner is destroyed, all the execute command resend will be stopped.
     */
    public Strategy setResend(@Extend.Resend int resend) {
        this.resend = resend;
        return this;
    }

    public Strategy setCallAsync(boolean async) {
        this.callAsync = async;
        return this;
    }

    public String getAuthority() {
        return authority;
    }

    public boolean isCallAsync() {
        return callAsync;
    }
}
