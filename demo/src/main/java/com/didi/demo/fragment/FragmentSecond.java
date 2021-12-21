package com.didi.demo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;
import com.didi.drouter.demo.R;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/8/31
 */
@Router(path = "/fragment/second")
public class FragmentSecond extends Fragment {

    public FragmentSecond() {
        RouterLogger.getAppLogger().d("SecondFragment 实例化");
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
                        .start(getContext(), new RouterCallback.ActivityCallback() {
                            @Override
                            public void onActivityResult(int resultCode, Intent data) {
                                if (data != null) {
                                    RouterLogger.toast(data.getStringExtra("result"));
                                }
                            }
                        });
            }
        });

        ((TextView)view.findViewById(R.id.title)).setText(getArguments().getString(Extend.REQUEST_BUILD_URI));
        return view;
    }

}
