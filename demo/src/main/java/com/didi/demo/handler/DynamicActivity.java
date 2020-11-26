package com.didi.demo.handler;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.demo.R;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.store.IRegister;
import com.didi.drouter.store.RouterKey;
import com.didi.drouter.store.ServiceKey;
import com.didi.drouter.utils.RouterLogger;

@Router(path = "/activity/dynamic")
public class DynamicActivity extends AppCompatActivity {

    IRegister iRegister1;
    IRegister iRegister2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic);

        iRegister1 = DRouter.register(RouterKey.build("/dynamic/handler").setLifecycleOwner(this),
                new IRouterHandler() {
                    @Override
                    public void handle(@NonNull Request request, @NonNull Result result) {
                        RouterLogger.toast("动态Handler执行成功");
                    }
                });
        iRegister2 = DRouter.register(ServiceKey.build(IDynamicService.class).setLifecycleOwner(this),
                new IDynamicService() {
                    @Override
                    public void execute() {
                        RouterLogger.toast("动态Service执行成功");
                    }
                });

        if (iRegister1.isSuccess() && iRegister2.isSuccess()) {
            RouterLogger.toast("动态注册成功");
        }

        findViewById(R.id.click_handler).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DRouter.build("/dynamic/handler").start(DynamicActivity.this);
            }
        });

        findViewById(R.id.click_service).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IDynamicService service = DRouter.build(IDynamicService.class).getService();
                service.execute();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 使用了LifecycleOwner，无需解注册
        // iRegister.unregister();
    }

    interface IDynamicService {
        void execute();
    }

}
