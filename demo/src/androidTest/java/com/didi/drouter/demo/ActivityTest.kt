package com.didi.drouter.demo

import android.app.Activity
import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.didi.drouter.api.DRouter
import com.didi.drouter.demo.activity.ActivityTest1
import com.didi.drouter.demo.activity.ActivityTest2
import com.didi.drouter.demo.activity.ActivityTest3
import com.didi.drouter.demo.util.EspressoTestUtil
import com.didi.drouter.router.RouterHelper
import org.hamcrest.CoreMatchers
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Test Activity related function is ok
 */
@RunWith(AndroidJUnit4::class)
class ActivityTest {

    //    /**
//     * Use [ActivityScenarioRule] to create and launch the activity under test before each test,
//     * and close it after each test. This is a replacement for
//     * [androidx.test.rule.ActivityTestRule].
//     */
//    @get:Rule var activityScenarioRule = activityScenarioRule<MainActivity>()
    @get:Rule
    var mainActivityTestRule = ActivityScenarioRule(MainActivity::class.java)


    @Test
    fun test_ActivityTest1PassArgs() {
        Intents.init()
        onView(withId(R.id.start_activity1)).perform(ViewActions.click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals(intent.component?.className, ActivityTest1::class.java.name)

        val extras = intent.extras
        Assert.assertTrue(extras?.getString("Arg1") == "Value1")
        Assert.assertTrue(extras?.getString("Arg2") == "Value2")
        Assert.assertTrue(extras?.getString("Arg3") == "Value3")
        Assert.assertTrue(extras?.getString("Arg4") == "Value4")

        Intents.release()
    }

    @Test
    fun test_ActivityTest2PassThroughScheme() {
        Intents.init()

        onView(withId(R.id.start_activity2)).perform(ViewActions.click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals(intent.component?.className, ActivityTest2::class.java.name)

        val extras = intent.extras
        Assert.assertTrue(extras?.getString("key") == "value")
        Intents.release()
    }

    @Test
    fun test_ActivityTest3Hold() {
        Intents.init()
        onView(withId(R.id.start_activity3)).perform(ViewActions.click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals(intent.component?.className, ActivityTest3::class.java.name)
        intent.extras.apply {
            Assert.assertTrue(this?.getString("a") == "我是a")
            Assert.assertTrue(this?.getString("b") == "我是b")
        }
        onView(withId(R.id.test3_text)).check(matches(isDisplayed()))
        Intents.release()

        // Test hold time not arrive
        Thread.sleep(1000L)
        Assert.assertNotNull(RouterHelper.getRequest("0"))

        // Test hold time is arrived
        Thread.sleep(2000L)
        onView(isRoot()).perform(ViewActions.pressBack())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("hold"))
    }

    @Test
    fun test_ActivityRouterNotFound() {
        onView(withId(R.id.start_activity_no)).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("router not found"))
    }

    @Test
    fun test_StartActivityForResultByLauncher() {
        onView(withId(R.id.start_activity_for_result)).perform(ViewActions.click())
        onView(withText("返回结果")).check(matches(isDisplayed()))

        onView(withText("返回结果")).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.containsString("成功获取到ActivityResult"))
    }

    @Test
    fun test_StartActivityButIntercept() {
        mainActivityTestRule.scenario.onActivity { att ->
            DRouter.build("/activity/interceptor")
                .start(att) { result ->
                    Assert.assertEquals(result.statusCode, 404)
                }
        }
    }

    @Test
    fun test_StartActivityForResultByIntent() {
        onView(withId(R.id.start_activity_result_intent)).perform(ViewActions.click())
        onView(withText("返回结果")).check(matches(isDisplayed()))

        onView(withText("返回结果")).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.containsString("成功获取到ActivityResult"))
    }
}