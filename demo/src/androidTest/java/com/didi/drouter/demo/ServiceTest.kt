package com.didi.drouter.demo


import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
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
class ServiceTest{
    @get:Rule
    var mainActivityTestRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun test_startServiceTestIServiceTest(){
        Espresso.onView(ViewMatchers.withId(R.id.start_service)).perform(ViewActions.scrollTo())
        Espresso.onView(ViewMatchers.withId(R.id.start_service)).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("1"))
    }
    @Test
    fun test_startServiceTestIServiceTestWithFilter(){
        Espresso.onView(ViewMatchers.withId(R.id.start_service_feature)).perform(ViewActions.scrollTo())
        Espresso.onView(ViewMatchers.withId(R.id.start_service_feature)).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("2"))
    }
    @Test
    fun test_startServiceTestICallService(){
        Espresso.onView(ViewMatchers.withId(R.id.start_service_call)).perform(ViewActions.scrollTo())
        Espresso.onView(ViewMatchers.withId(R.id.start_service_call)).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("3"))
    }

    @Test
    fun test_startServiceTestAnyAbility(){
        Espresso.onView(ViewMatchers.withId(R.id.start_service_any)).perform(ViewActions.scrollTo())
        Espresso.onView(ViewMatchers.withId(R.id.start_service_any)).perform(ViewActions.click())
        EspressoTestUtil.matchToolbarTitle(CoreMatchers.endsWith("4"))
    }
}