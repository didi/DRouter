package com.didi.drouter.router;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by gaowei on 2019/1/27
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({
        RouterState.OK,
        RouterState.NOT_FOUND,
        RouterState.INTERCEPT})
public @interface RouterState {

    int OK           = 0;
    int NOT_FOUND    = 1;
    int INTERCEPT    = 2;
}
