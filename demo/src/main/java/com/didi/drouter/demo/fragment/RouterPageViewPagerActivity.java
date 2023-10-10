package com.didi.drouter.demo.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.didi.drouter.demo.R;
import com.didi.drouter.annotation.Router;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.page.IPageBean;
import com.didi.drouter.page.IPageRouter;
import com.didi.drouter.page.RouterPageViewPager;
import com.didi.drouter.store.ServiceKey;
import com.didi.drouter.utils.RouterLogger;

@Router(path = "/activity/router_page_viewpager")
public class RouterPageViewPagerActivity extends AppCompatActivity {

    RouterPageViewPager pageRouter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        ViewPager viewPager = new ViewPager(this);
        viewPager.setId(R.id.drouter_view_pager);
        ((ViewGroup)findViewById(R.id.fragment_container)).addView(viewPager);

        pageRouter = new RouterPageViewPager(getSupportFragmentManager(), viewPager, 1);
        DRouter.register(
                ServiceKey.build(IPageRouter.class).setAlias("router_page_viewpager").setLifecycle(getLifecycle()),
                pageRouter);


        pageRouter.update(
                new IPageBean.DefPageBean("/fragment/first/0"),
                new IPageBean.DefPageBean("/fragment/first/1"),
                new IPageBean.DefPageBean("/fragment/first/2"),
                new IPageBean.DefPageBean("/fragment/first/3"),
                new IPageBean.DefPageBean("/fragment/first/4"),
                new IPageBean.DefPageBean("/fragment/first/5"),
                new IPageBean.DefPageBean("/fragment/first/6"));

        pageRouter.addPageObserver(new IPageRouter.IPageObserver() {
            @Override
            public void onPageChange(@NonNull IPageBean from, @NonNull IPageBean to, int type) {
                RouterLogger.getAppLogger().d(from.getPageUri() +  " -> " + to.getPageUri() + "  type:" + type);
            }
        }, false, this);

        ((TextView)findViewById(R.id.btn1)).setText("修改数据");
        ((TextView)findViewById(R.id.btn2)).setText("切换页面");
    }

    public void onClick1(View view) {
        pageRouter.update(
                new IPageBean.DefPageBean("/fragment/first/0-"),
                new IPageBean.DefPageBean("/fragment/first/1-"),
                new IPageBean.DefPageBean("/fragment/first/2-"));
    }

    public void onClick2(View view) {
        IPageRouter router = DRouter.build(IPageRouter.class).setAlias("router_page_viewpager").getService();
        router.showPage(new IPageBean.DefPageBean("/fragment/first/5"));
    }
}
