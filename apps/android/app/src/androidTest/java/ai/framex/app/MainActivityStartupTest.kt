package ai.framex.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityStartupTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, true, true)

    @Test
    fun mainActivityStartsWithoutCrashing() {
        check(!activityRule.activity.isFinishing)
        check(!activityRule.activity.isDestroyed)
    }
}
