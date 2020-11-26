package com.didi.demo.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/3
 */
@Router(path = "/view/bottom")
public class BottomView extends View {
    public BottomView(Context context) {
        this(context, null);
    }

    public BottomView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        RouterLogger.getAppLogger().d("BottomView 实例化");

    }
}
