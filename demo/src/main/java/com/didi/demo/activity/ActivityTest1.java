package com.didi.demo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.didi.demo.interceptor.InterceptorTest1;
import com.didi.demo.interceptor.InterceptorTest2;
import com.didi.demo.interceptor.OutClass;
import com.didi.drouter.annotation.Router;
import com.didi.drouter.demo.R;

@Router(path = "/activity/test1",
        interceptor = {InterceptorTest1.class, InterceptorTest2.class, OutClass.InnerInterceptor.class})
public class ActivityTest1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_1);
    }
}
