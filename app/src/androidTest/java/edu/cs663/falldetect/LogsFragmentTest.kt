package edu.cs663.falldetect

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.cs663.falldetect.ui.MainActivity
import edu.cs663.falldetect.util.PrefsHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for LogsFragment and Emergency Dialog.
 * Tests the "Detect Fall" button and emergency countdown functionality.
 */
@RunWith(AndroidJUnit4::class)
class LogsFragmentTest {

    private lateinit var context: Context
    private lateinit var prefsHelper: PrefsHelper

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefsHelper = PrefsHelper(context)
        
        // Set a known countdown duration for testing
        prefsHelper.saveTimerDuration(10)
    }

    @After
    fun cleanup() {
        // Reset to default
        prefsHelper.saveTimerDuration(15)
    }

    @Test
    fun testDetectFallButtonDisplayed() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Logs
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify Detect Fall button is displayed
        onView(withId(R.id.btn_detect_fall))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("Detect Fall"))))
    }

    @Test
    fun testDetectFallButtonOpensDialog() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Logs
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Click Detect Fall button
        onView(withId(R.id.btn_detect_fall))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify emergency dialog is displayed
        onView(withId(R.id.text_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("Fall Detected"))))
        
        // Verify countdown value is displayed
        onView(withId(R.id.text_countdown_value))
            .check(matches(isDisplayed()))
        
        // Verify progress indicator is displayed
        onView(withId(R.id.progress_countdown))
            .check(matches(isDisplayed()))
        
        // Verify I'm OK button is displayed
        onView(withId(R.id.btn_cancel))
            .check(matches(isDisplayed()))
            .check(matches(withText("I'm OK")))
    }

    @Test
    fun testEmergencyDialogCountdownStarts() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Logs
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Click Detect Fall button
        onView(withId(R.id.btn_detect_fall))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify initial countdown value (should be 10 from setup)
        onView(withId(R.id.text_countdown_value))
            .check(matches(isDisplayed()))
        
        // Wait for countdown to tick
        Thread.sleep(2000)
        
        // Countdown should still be visible (not dismissed yet)
        onView(withId(R.id.text_countdown_value))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEmergencyDialogCancelButton() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Logs
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Click Detect Fall button
        onView(withId(R.id.btn_detect_fall))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify dialog is displayed
        onView(withId(R.id.text_title))
            .check(matches(isDisplayed()))
        
        // Click cancel button
        onView(withId(R.id.btn_cancel))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify dialog is dismissed (Detect Fall button should be visible again)
        onView(withId(R.id.btn_detect_fall))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEmergencyDialogUsesPersistedDuration() {
        // Set custom duration
        prefsHelper.saveTimerDuration(20)
        
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Logs
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Click Detect Fall button
        onView(withId(R.id.btn_detect_fall))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify countdown starts at 20 (or close to it)
        onView(withId(R.id.text_countdown_value))
            .check(matches(isDisplayed()))
        
        // Progress max should be 20
        onView(withId(R.id.progress_countdown))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEmergencyDialogDemoNoteDisplayed() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Logs
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Click Detect Fall button
        onView(withId(R.id.btn_detect_fall))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify demo note is displayed
        onView(withId(R.id.text_demo_note))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("Demo only"))))
    }

    @Test
    fun testEmergencyDialogProgressRing() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Logs
        onView(withId(R.id.logsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Click Detect Fall button
        onView(withId(R.id.btn_detect_fall))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify progress ring is displayed
        onView(withId(R.id.progress_countdown))
            .check(matches(isDisplayed()))
        
        // Wait for countdown to tick
        Thread.sleep(2000)
        
        // Progress ring should still be visible
        onView(withId(R.id.progress_countdown))
            .check(matches(isDisplayed()))
    }
}

