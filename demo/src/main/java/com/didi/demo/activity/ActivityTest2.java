package com.didi.demo.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.Extend;
import com.didi.drouter.demo.R;

@Router(scheme = "didi", host = "www\\.didi\\.com", path = "/activity/test2")
public class ActivityTest2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_2);


        TextView textView = findViewById(R.id.test2_text);
        String primaryUrl = getIntent().getStringExtra(Extend.REQUEST_BUILD_URI);
        String argUrl = getIntent().getStringExtra("argUrl");
        textView.setText(String.format("BUILD_URI=%s\n\nargUrl=%s\na=%s\nb=%s",
                Uri.decode(primaryUrl),
                Uri.decode(argUrl),
                getIntent().getStringExtra("a"),
                getIntent().getStringExtra("b")));
    }
}
