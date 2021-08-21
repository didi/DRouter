package com.didi.demo.activity;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.didi.demo.interceptor.OutClass;
import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.Extend;
import com.didi.drouter.demo.R;

@Router(uri = "/activity/Test1_<Arg1>_<Arg2>",
        interceptorName = {"interceptor1", "interceptor2"},
        interceptor = {OutClass.InnerInterceptor.class})
public class ActivityTest1 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_1);

        TextView textView = findViewById(R.id.test1_text);
        String primaryUrl = getIntent().getStringExtra(Extend.REQUEST_BUILD_URI);
        textView.setText(String.format("BUILD_URI=%s\n\nArg1=%s\nArg2=%s\nArg3=%s\nArg4=%s",
                primaryUrl,
                getIntent().getStringExtra("Arg1"),
                getIntent().getStringExtra("Arg2"),
                getIntent().getStringExtra("Arg3"),
                getIntent().getStringExtra("Arg4")));
    }
}
