package com.didi.drouter.demo


import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.didi.drouter.demo.web.WebActivity
import org.hamcrest.Matchers.containsString
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Test Activity related function is ok
 */
@RunWith(AndroidJUnit4::class)
class WebViewTest {
    @get:Rule
    var mainActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

    private fun ensureWebActivityLaunched() {
        mainActivityTestRule.scenario.onActivity { att->
            val intent = Intent(att, WebActivity::class.java).apply {
                putExtra("url", "file:///android_asset/scheme-test.html")
            }
            att.startActivity(intent)
        }
        Espresso.onView(ViewMatchers.withId(R.id.webview))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onWebView(ViewMatchers.withId(R.id.webview)).forceJavascriptEnabled()
    }

    @Test
    fun test_startWebViewToTestActivity1PassArgs() {
        ensureWebActivityLaunched()

        val p1 = "//a[contains(text(),'Test1_a_b')]"
        onWebView()
            .withElement(findElement(Locator.XPATH, p1)) // similar to onView(withId(...))
            .perform(webClick()) // Similar to perform(click())

        onView(withId(R.id.test1_text)).check(matches(withText(containsString("Arg1=a\n" +
                "Arg2=b\n" +
                "Arg3=null\n" +
                "Arg4=null"))))
    }

    @Test
    fun test_startWebViewToTestActivity2PassThroughScheme() {
        ensureWebActivityLaunched()
        val p2 = "//a[contains(text(),'argUrl')]"
        onWebView()
            .withElement(findElement(Locator.XPATH, p2)) // similar to onView(withId(...))
            .perform(webClick()) // Similar to perform(click())

        onView(withId(R.id.test2_text)).check(matches(withText(containsString("argUrl=http://m.didi.com\n" +
                "a=1\n" +
                "b=222"))))
    }

    @Test
    fun test_startWebViewToLaunchHandleTest2() {
        ensureWebActivityLaunched()
        // check the intent is started correctly
        Intents.init()
        val p2 = "//a[contains(text(),'handler')]"
        onWebView()
            .withElement(findElement(Locator.XPATH, p2)) // similar to onView(withId(...))
            .perform(webClick()) // Similar to perform(click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals("didi://router/handler/test2", intent.data?.toString())

        Intents.release()
        // check the handle is started correctly
        // TODO 当前不支持检测 Toast
    }
}