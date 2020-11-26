package com.didi.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.demo.R;

@Router(path = "/activity/result")
public class ActivityResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
    }

    public void onClick(View view) {
        Intent intent = getIntent();
        intent.putExtra("result", "成功获取到ActivityResult");
        setResult(RESULT_OK, intent);
        finish();
    }
}
