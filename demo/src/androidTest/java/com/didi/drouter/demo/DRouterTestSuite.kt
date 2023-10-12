package com.didi.drouter.demo

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(Suite::class)
@SuiteClasses(
    ActivityTest::class,
    FragmentTest::class,
    ServiceTest::class,
    WebViewTest::class,
)
class DRouterTestSuite