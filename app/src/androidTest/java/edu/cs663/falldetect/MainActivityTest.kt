package edu.cs663.falldetect

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import edu.cs663.falldetect.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation smoke test for MainActivity.
 * Verifies that the app launches and navigation works.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    
    @Test
    fun testActivityLaunches() {
        // Launch MainActivity
        ActivityScenario.launch(MainActivity::class.java)
        
        // Verify bottom navigation is displayed
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
        
        // Verify nav host fragment is displayed
        onView(withId(R.id.nav_host_fragment))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testNavigationToSettings() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Click on Settings tab
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        // Verify settings title is displayed
        onView(withId(R.id.settings_title))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testNavigationToLogs() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Click on Logs tab
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        // Verify logs title is displayed
        onView(withId(R.id.logs_title))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testNavigationBackToHome() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        // Navigate back to Home
        onView(withId(R.id.homeFragment))
            .perform(click())

        // Verify home elements are displayed
        onView(withId(R.id.preview_view))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btn_start_monitoring))
            .check(matches(isDisplayed()))
    }
}

