package com.didi.demo.web;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;


/**
 * Created by gaowei on 2018/9/1
 */
public class SchemeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DRouter.build(getIntent().getData().toString())
                // 可选，只要以didi://router开头的请求，会获取path并去匹配只有path注解的Activity
                .putExtra(Extend.START_ACTIVITY_WITH_DEFAULT_SCHEME_HOST, "didi://router")
                .start(this);
        finish();
    }
}
