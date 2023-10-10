package com.didi.drouter.demo

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.didi.drouter.demo.activity.ActivityTest1
import com.didi.drouter.demo.activity.ActivityTest2
import com.didi.drouter.demo.activity.ActivityTest3
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
    fun test_ActivityTest1(){
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
    fun test_ActivityTest2(){
        Intents.init()

        onView(withId(R.id.start_activity2)).perform(ViewActions.click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals(intent.component?.className, ActivityTest2::class.java.name)

        val extras = intent.extras
        Assert.assertTrue(extras?.getString("key") == "value")



        Intents.release()
        // TODO test toast is showing
    }

    @Test
    fun test_ActivityTest3() {
        Intents.init()
        onView(withId(R.id.start_activity3)).perform(ViewActions.click())

        val intent = Intents.getIntents()[0]
        Assert.assertEquals(intent.component?.className, ActivityTest3::class.java.name)

        val extras = intent.extras
        Assert.assertTrue(extras?.getString("a") == "我是a")
        Assert.assertTrue(extras?.getString("b") == "我是b")


        Intents.release()


        // TODO test toast is showing
//        Thread.sleep(2000L)
//        onView(withText("hold activity is started")).check(matches(isDisplayed()))
    }
}