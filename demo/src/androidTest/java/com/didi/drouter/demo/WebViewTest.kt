package com.didi.drouter.demo


import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.webClick
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.didi.drouter.api.DRouter
import com.didi.drouter.demo.activity.ActivityTest1
import com.didi.drouter.demo.activity.ActivityTest2
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
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

    private fun scrollAndStartWebActivity() {
        Espresso.onView(ViewMatchers.withId(R.id.start_webview)).perform(ViewActions.scrollTo())
        Espresso.onView(ViewMatchers.withId(R.id.start_webview)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.webview))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_startWebViewToTestActivity1PassArgs() {
        scrollAndStartWebActivity()
        Intents.init()

        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "h2+p:first-child")) // similar to onView(withId(...))
            .perform(webClick()) // Similar to perform(click())
            // Similar to check(matches(...))
//            .check(webMatches(getCurrentUrl(), containsString("navigation_2.html")))

        Espresso.onView(ViewMatchers.withId(R.id.start_activity1)).perform(ViewActions.click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals(intent.component?.className, ActivityTest1::class.java.name)

        val extras = intent.extras
        Assert.assertTrue(extras?.getString("Arg1") == "Value1")
        Assert.assertTrue(extras?.getString("Arg2") == "Value2")
        Assert.assertTrue(extras?.getString("Arg3") == null)
        Assert.assertTrue(extras?.getString("Arg4") == null)

        Intents.release()
    }

    @Test
    fun test_startWebViewToTestActivity2PassThroughScheme() {
        scrollAndStartWebActivity()
        Intents.init()

        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "h2+p:first-child")) // similar to onView(withId(...))
            .perform(webClick()) // Similar to perform(click())
            // Similar to check(matches(...))
//            .check(webMatches(getCurrentUrl(), containsString("navigation_2.html")))

        Espresso.onView(ViewMatchers.withId(R.id.start_activity2)).perform(ViewActions.click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals(intent.component?.className, ActivityTest2::class.java.name)

        intent.extras.apply {
            Assert.assertTrue(this?.getString("argUrl") == "http:/mdidi.com")
            Assert.assertTrue(this?.getString("a") == "1")
            Assert.assertTrue(this?.getString("b") == "222")
        }
        Intents.release()
    }

    @Test
    fun test_startWebViewToLaunchHandleTest2() {
        scrollAndStartWebActivity()
        Intents.init()

        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, "h2+p:first-child"))
            .perform(webClick())

     // TODO
    }
}