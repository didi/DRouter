package com.didi.drouter.demo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.didi.drouter.api.DRouter
import com.didi.drouter.api.Extend
import com.didi.drouter.module_base.ParamObject
import com.didi.drouter.module_base.service.IServiceTest
import com.didi.drouter.module_base.service.IServiceTest2
import com.didi.drouter.module_base.service.ServiceFeature
import com.didi.drouter.service.ICallService
import com.didi.drouter.utils.RouterLogger


/**
 * Created by gaowei on 2018/9/1
 */
class MainActivity : AppCompatActivity() {

    private var launcher: ActivityResultLauncher<Intent>? = null

    private lateinit var toolbar: ActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val contentView = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        setContentView(contentView)

        toolbar = supportActionBar!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (window.decorView as? FrameLayout)?.getChildAt(0)?.also {
                ViewCompat.setOnApplyWindowInsetsListener(it) { v, windowInsets ->
                    val systemBar = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                    v.setPaddingRelative(
                        0,
                        systemBar.top,
                        0,
                        0
                    )
                    WindowInsetsCompat.CONSUMED
                }
            }
        }

        launcher = registerForActivityResult(MyResultContract()) {
            it?.let {
                RouterLogger.toast(it)
                appendToToolbarTitle(it)
            }
        }
    }

    class MyResultContract : ActivityResultContract<Intent, String?>() {
        override fun createIntent(context: Context, input: Intent): Intent {
            return input
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return intent?.getStringExtra("result")
        }
    }

    fun onClick(view: View) {
        resetToolbarTitle()
        when (view.id) {
            R.id.start_activity1 -> DRouter.build("/activity/Test1_Value1_Value2?Arg3=Value3")
                .putExtra("Arg4", "Value4")
                .start(this)

            R.id.start_activity2 -> {
                val url = Uri.encode("http://m.didi.com?arg1=2&arg2=3#fragment1")
                DRouter.build(
                    String.format(
                        "didi://www.didi.com/activity/test2?argUrl=%s&a=1&b=222#fragment2",
                        url
                    )
                )
                    .putExtra("key", "value")
                    .start(this) { result ->
                        if (result.isActivityStarted) {
                            //val bundle = result.getExtra()
                            //val value = bundle.getString("key")
                            RouterLogger.toast("打开成功")
                        }
                    }
            }

            R.id.start_activity3 -> DRouter.build("/activity/test3")
                .putExtra("a", "我是a")
                .putExtra("b", "我是b")
                .start(this) { result ->
                    if (result.isActivityStarted) {
                        appendToToolbarTitle("hold")
                        Toast.makeText(
                            this@MainActivity,
                            "hold activity is started", Toast.LENGTH_LONG
                        ).show()
                    }
                }

            R.id.start_activity_no -> DRouter.build("/activity/no").start(this) { result ->
                if (!result.isActivityStarted) {
                    appendToToolbarTitle("router not found")
                    Toast.makeText(this@MainActivity, "router not found", Toast.LENGTH_LONG).show()
                }
            }

            R.id.start_activity_for_result ->
                DRouter.build("/activity/result")
                    .setActivityResultLauncher(launcher)
                    .start(this@MainActivity)

            R.id.start_but_intercept -> {
                DRouter.build("/activity/interceptor")
                    .start(this) { result ->
                        Toast.makeText(
                            this@MainActivity,
                            "状态码: ${result.statusCode}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            R.id.start_activity_result_intent -> {
                // 兼容通过intent启动Activity
                val intent = Intent("com.intent.activity")
                DRouter.build("")
                    .putExtra(Extend.START_ACTIVITY_VIA_INTENT, intent)
                    .setActivityResultLauncher(launcher)
                    .start(this)
            }

            R.id.start_fragment1 -> DRouter.build("/fragment/first/1").start(this) { result ->
                if (result.fragment != null) {
                    appendToToolbarTitle("获取FirstFragment成功")
                    Toast.makeText(
                        DRouter.getContext(),
                        "获取FirstFragment成功",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            R.id.start_view1 -> DRouter.build("/view/headview").start(this) { result ->
                if (result.view != null) {
                    appendToToolbarTitle("获取HeadView成功")
                    Toast.makeText(DRouter.getContext(), "获取HeadView成功", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            R.id.start_router_page_viewpager -> DRouter.build("/activity/router_page_viewpager")
                .start(this)

            R.id.start_router_page_single -> DRouter.build("/activity/router_page_single")
                .start(this)

            R.id.start_router_page_stack -> DRouter.build("/activity/router_page_stack").start(this)
            R.id.start_handler1 -> DRouter.build("/handler/test1").start(this)
            R.id.start_handler2 -> DRouter.build("didi://router/handler/test2").start(this) { }
            R.id.start_handler3 ->                 // 延迟5s返回结果，目标异步返回结束不阻塞当前线程
                DRouter.build("/handler/test3")
                    .setLifecycle(lifecycle) //绑定生命周期，防止内存泄漏
                    .setHoldTimeout(1000) // 设置超时时间
                    .start(this) { RouterLogger.toast("HandlerTest3执行结束") }

            R.id.start_webview -> DRouter.build("/activity/webview")
                .putExtra("url", "file:///android_asset/scheme-test.html")
                .start(this)

            R.id.start_service -> {
                val res = DRouter.build(IServiceTest::class.java).setAlias("test1")
                    .getService().test()
                appendToToolbarTitle(res.toString())
            }

            R.id.start_service_feature -> {
                val bean = ServiceFeature().apply {
                    a = 1
                    a1 = 2
                    b = 3
                    c = 1
                    d = 2
                    e = '3'
                    e1 = '1'
                    f = 1f
                    g = 2.0
                    h = true
                    i = "1"
                    j = "2"
                }
                val res = DRouter.build(IServiceTest::class.java)
                    .setAlias("test2")
                    .setFeature(bean)
                    .getService(3, "a", booleanArrayOf(true), this)
                    .test()
                appendToToolbarTitle(res.toString())
            }

            R.id.start_service_call -> {
                val res = DRouter.build(ICallService::class.java)
                    .setAlias("login")
                    .getService()
                    .call<Any>(ParamObject(), 3)
                appendToToolbarTitle(res.toString())
            }

            R.id.start_service_any -> {
                val res= DRouter.build(IServiceTest2::class.java)
                    .setAlias("name1")
                    .getService()
                    .test2()
                appendToToolbarTitle(res.toString())
            }

            R.id.start_dynamic_register -> DRouter.build("/activity/dynamic").putExtra("type", 2)
                .start(this)

            R.id.start_remote_page -> DRouter.build("/activity/remote_test_activity").start(this)
            else -> {
            }
        }
    }

    private fun resetToolbarTitle() {
        toolbar.title = getString(R.string.app_name)
    }

    private fun appendToToolbarTitle(text: String) {
        toolbar.title = "${toolbar.title}$text"
    }

}