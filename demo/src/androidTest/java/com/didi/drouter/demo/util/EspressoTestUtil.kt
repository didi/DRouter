package com.didi.drouter.demo.util

import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher

object EspressoTestUtil{

    fun matchToolbarTitle(textMatcher: Matcher<String>): ViewInteraction {
        return Espresso.onView(ViewMatchers.isAssignableFrom(Toolbar::class.java))
            .check(ViewAssertions.matches(withToolbarTitle(textMatcher)))
    }

    fun withToolbarTitle(textMatcher: Matcher<String>): Matcher<Any> {
        return object : BoundedMatcher<Any, Toolbar>(Toolbar::class.java) {
            public override fun matchesSafely(toolbar: Toolbar): Boolean {
                return textMatcher.matches(toolbar.title)
            }

            override fun describeTo(description: Description) {
                description.appendText("with toolbar title: ")
                textMatcher.describeTo(description)
            }
        }
    }

}