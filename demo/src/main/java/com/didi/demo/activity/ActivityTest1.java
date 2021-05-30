package com.didi.demo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.didi.demo.interceptor.OutClass;
import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.Extend;
import com.didi.drouter.demo.R;

@Router(path = "/activity/test1_<arg1>_<arg2>",
        interceptorName = {"interceptor1", "interceptor2"},
        interceptor = {OutClass.InnerInterceptor.class})
public class ActivityTest1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_1);

        TextView textView = findViewById(R.id.test1_text);
        String primaryUrl = getIntent().getStringExtra(Extend.REQUEST_BUILD_URI);
        textView.setText(String.format("BUILD_URI=%s\n\narg1=%s\narg2=%s\narg3=%s\narg4=%s",
                primaryUrl,
                getIntent().getStringExtra("arg1"),
                getIntent().getStringExtra("arg2"),
                getIntent().getStringExtra("arg3"),
                getIntent().getStringExtra("arg4")));
    }
}
