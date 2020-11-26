package com.didi.demo.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.demo.R;
import com.didi.drouter.store.ServiceKey;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.page.IPageBean;
import com.didi.drouter.page.IPageRouter;
import com.didi.drouter.page.RouterPageStack;

@Router(path = "/activity/router_page_stack")
public class RouterPageStackActivity extends AppCompatActivity {

    private int pageNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        RouterPageStack pageRouter = new RouterPageStack(getSupportFragmentManager(), R.id.fragment_container);
        pageRouter.addPageObserver(new IPageRouter.IPageObserver() {
            @Override
            public void onPageChange(@NonNull IPageBean from, @NonNull IPageBean to) {
                RouterLogger.getAppLogger().d(from.getPageUri() +  " -> " + to.getPageUri());
            }
        }, this);
        DRouter.register(
                ServiceKey.build(IPageRouter.class).setAlias("router_page_stack").setLifecycleOwner(this),
                pageRouter);

        ((TextView)findViewById(R.id.btn1)).setText("添加页面");
        ((TextView)findViewById(R.id.btn2)).setText("移除页面");
    }

    public void onClick1(View view) {
        IPageRouter router = DRouter.build(IPageRouter.class).setAlias("router_page_stack").getService();
        router.showPage(new IPageBean.DefPageBean("/fragment/first/" + pageNum));
        pageNum++;
    }

    public void onClick2(View view) {
        IPageRouter router = DRouter.build(IPageRouter.class).setAlias("router_page_stack").getService();
        router.popPage();
    }
}
