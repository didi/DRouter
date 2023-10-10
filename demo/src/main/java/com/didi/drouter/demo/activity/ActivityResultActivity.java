package com.didi.drouter.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

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
