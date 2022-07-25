package com.didi.demo.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;
import com.didi.drouter.demo.R;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/8/31
 */
@Router(path = "/fragment/second")
public class FragmentSecond extends Fragment {

    private ActivityResultLauncher<Intent> launcher;

    public FragmentSecond() {
        RouterLogger.getAppLogger().d("SecondFragment 实例化");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launcher = registerForActivityResult(new ActivityResultContract<Intent, String>() {
            @NonNull
            @Override
            public Intent createIntent(@NonNull Context context, Intent input) {
                return input;
            }

            @Override
            public String parseResult(int resultCode, @Nullable Intent intent) {
                return intent != null ? intent.getStringExtra("result") : "";
            }
        }, RouterLogger::toast);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_title, container, false);
        view.findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DRouter.build("/activity/result")
                        .setActivityResultLauncher(launcher)
                        .start(getContext());
            }
        });

        ((TextView)view.findViewById(R.id.title)).setText(getArguments().getString(Extend.REQUEST_BUILD_URI));
        return view;
    }

}
