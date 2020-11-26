package com.didi.demo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;
import com.didi.drouter.demo.R;
import com.didi.drouter.module_base.ParamObject;
import com.didi.drouter.module_base.remote.IRemoteFunction;
import com.didi.drouter.module_base.remote.RemoteFeature;
import com.didi.drouter.module_base.service.IServiceTest;
import com.didi.drouter.module_base.service.IServiceTest2;
import com.didi.drouter.module_base.service.ServiceFeature;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.service.ICallService;
import com.didi.drouter.utils.RouterLogger;

/**
 * Created by gaowei on 2018/9/1
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.start_activity1:
                DRouter.build("/activity/test1").putExtra("key", "value").start(this);
                break;

            case R.id.start_activity2:
                String url = Uri.encode("http://m.didi.com?arg1=2&arg2=3#fragment1");
                DRouter.build(String.format("didi://www.didi.com/activity/test2?argUrl=%s&a=1&b=222#fragment2", url))
                        .putExtra("key", "value")
                        .start(this, new RouterCallback() {
                            @Override
                            public void onResult(@NonNull Result result) {
                                if (result.isActivityStarted()) {
                                    Bundle bundle = result.getExtra();
                                    String value = bundle.getString("key");
                                    RouterLogger.toast("打开成功");
                                }
                            }
                        });
                break;

            case R.id.start_activity3:
                DRouter.build("/activity/test3")
                        .putExtra("a", "我是a")
                        .putExtra("b", "我是b")
                        .start(this, new RouterCallback() {
                            @Override
                            public void onResult(@NonNull Result result) {
                                if (result.isActivityStarted()) {
                                    Toast.makeText(MainActivity.this,
                                            "hold activity is started", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                break;

            case R.id.start_activity_no:
                DRouter.build("/activity/no").start(this, new RouterCallback() {
                    @Override
                    public void onResult(@NonNull Result result) {
                        if (!result.isActivityStarted()) {
                            Toast.makeText(MainActivity.this, "router not found", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                break;

            case R.id.start_activity_for_result:
                DRouter.build("/activity/result")
                        .start(MainActivity.this, new RouterCallback.ActivityCallback() {
                            @Override
                            public void onActivityResult(int resultCode, Intent data) {
                                if (data != null) {
                                    RouterLogger.toast(data.getStringExtra("result"));
                                }
                            }
                        });
                break;

            case R.id.start_activity_result_intent:
                // 兼容通过intent启动Activity
                Intent intent = new Intent("com.intent.activity");
                DRouter.build("")
                        .putExtra(Extend.START_ACTIVITY_VIA_INTENT, intent)
                        .start(this, new RouterCallback.ActivityCallback() {
                            @Override
                            public void onActivityResult(int resultCode, Intent data) {
                                if (data != null) {
                                    RouterLogger.toast(data.getStringExtra("result"));
                                }
                            }
                        });
                break;

            case R.id.start_fragment1:
                DRouter.build("/fragment/first/1").start(this, new RouterCallback() {
                    @Override
                    public void onResult(@NonNull Result result) {
                        if (result.getFragment() != null) {
                            Toast.makeText(DRouter.getContext(), "获取FirstFragment成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;

            case R.id.start_view1:
                DRouter.build("/view/headview").start(this, new RouterCallback() {
                    @Override
                    public void onResult(@NonNull Result result) {
                        if (result.getView() != null) {
                            Toast.makeText(DRouter.getContext(), "获取HeadView成功", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                break;

            case R.id.start_router_page_viewpager:
                DRouter.build("/activity/router_page_viewpager").start(this);
                break;

            case R.id.start_router_page_single:
                DRouter.build("/activity/router_page_single").start(this);
                break;

            case R.id.start_router_page_stack:
                DRouter.build("/activity/router_page_stack").start(this);
                break;

            case R.id.start_handler1:
                DRouter.build("/handler/test1").start(this);
                break;

            case R.id.start_handler2:
                DRouter.build("didi://router/handler/test2").start(this, new RouterCallback() {
                    @Override
                    public void onResult(@NonNull Result result) {
                    }
                });
                break;

            case R.id.start_handler3:
                // 延迟5s返回结果，目标异步返回结束不阻塞当前线程
                DRouter.build("/handler/test3")
                        .setLifecycleOwner(this)    //绑定生命周期，防止内存泄漏
                        .setHoldTimeout(1000)   // 设置超时时间
                        .start(this, new RouterCallback() {
                            @Override
                            public void onResult(@NonNull Result result) {
                                RouterLogger.toast("HandlerTest3执行结束");
                            }
                        });
                break;

            case R.id.start_webview:
                DRouter.build("/activity/webview")
                        .putExtra("url", "file:///android_asset/scheme-test.html")
                        .start(this);
                break;

            case R.id.start_service:
                DRouter.build(IServiceTest.class).setAlias("test1").getService().test();
                break;

            case R.id.start_service_feature:
                ServiceFeature bean = new ServiceFeature();
                bean.a = 1;
                bean.a1 = 2;
                bean.b = 3;
                bean.c = 1;
                bean.d = 2;
                bean.e = '3';
                bean.e1 = '1';
                bean.f = 1;
                bean.g = 2;
                bean.h = true;
                bean.i = "1";
                bean.j = "2";

                DRouter.build(IServiceTest.class)
                        .setAlias("test2")
                        .setFeature(bean)
                        .getService(3, "a", new boolean[]{true}, this)
                        .test();
                break;

            case R.id.start_service_call:
                DRouter.build(ICallService.class)
                        .setAlias("login")
                        .getService()
                        .call(new ParamObject(), 3);
                break;

            case R.id.start_service_any:
                DRouter.build(IServiceTest2.class)
                        .setAlias("name1")
                        .getService()
                        .test2();
                break;

            case R.id.start_dynamic_register:
                DRouter.build("/activity/dynamic").putExtra("type", 2).start(this);
                break;

            case R.id.start_remote_page:
                DRouter.build("/activity/remote").start(this);
                break;

            case R.id.start_remote_test:
                final RemoteFeature feature = new RemoteFeature();
                feature.a = 1;
                feature.b = "1";
                DRouter.build(IRemoteFunction.class)
                        .setFeature(feature)
                        .setAlias("remote")
                        .setRemoteAuthority("com.didi.drouter.remote.demo.remote")
                        .getService()
                        .call();
                break;
            default:
                break;
        }


    }
}
