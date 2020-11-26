package com.didi.demo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.demo.R;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.RouterHelper;

@Router(path = "/activity/test3", hold = true)
public class ActivityTest3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_3);

        Request request = RouterHelper.getRequest(this);
        if (request != null) {
            TextView textView = findViewById(R.id.test3_text);
            textView.setText(String.format("BUILD_URI=%s\n\na=%s\nb=%s",
                    request.getUri().toString(),
                    request.getExtra().getString("a"),
                    request.getExtra().getString("b")));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                RouterHelper.release(ActivityTest3.this);
            }
        }, 2000);
    }
}
