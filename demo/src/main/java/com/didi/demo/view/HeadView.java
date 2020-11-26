package com.didi.demo.view;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/3
 */
@Router(path = "/view/headview")
public class HeadView extends ViewGroup {

    public HeadView(Context context) {
        this(context, null);
    }

    public HeadView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeadView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        RouterLogger.getAppLogger().d("BottomView 实例化");
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark));
        test();
    }

    private void test() {

    }
}
