package com.didi.drouter.router;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by gaowei on 2019/1/27
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        RouterType.MULTIPLE,
        RouterType.UNDEFINED,
        RouterType.ACTIVITY,
        RouterType.FRAGMENT,
        RouterType.VIEW,
        RouterType.HANDLER})
public @interface RouterType {

    int MULTIPLE    = -1;
    int UNDEFINED   = 0;
    int ACTIVITY    = 1;
    int FRAGMENT    = 2;
    int VIEW        = 3;
    int HANDLER     = 4;
}
