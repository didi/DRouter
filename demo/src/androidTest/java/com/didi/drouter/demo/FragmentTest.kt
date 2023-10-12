package com.didi.drouter.demo


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.didi.drouter.demo.util.EspressoTestUtil
import org.hamcrest.CoreMatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Test Activity related function is ok
 */
@RunWith(AndroidJUnit4::class)
class FragmentTest {

    @get:Rule
    var mainActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun test_navigateToFragment() {
        onView(withId(R.id.start_fragment1)).perform(ViewActions.scrollTo())
        onView(withId(R.id.start_fragment1)).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("获取FirstFragment成功"))
    }

    @Test
    fun test_navigateToView() {
        onView(withId(R.id.start_view1)).perform(ViewActions.scrollTo())
        onView(withId(R.id.start_view1)).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("获取HeadView成功"))
    }
}