package edu.cs663.falldetect

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import edu.cs663.falldetect.ui.MainActivity
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for HomeFragment camera functionality.
 * Tests camera binding, FPS tracking, and start/stop monitoring.
 */
@RunWith(AndroidJUnit4::class)
class HomeFragmentCameraTest {
    
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    @Test
    fun testHomeFragmentLoadsWithPermissions() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Verify preview view is displayed
        onView(withId(R.id.preview_view))
            .check(matches(isDisplayed()))
        
        // Verify overlay view is displayed
        onView(withId(R.id.overlay_view))
            .check(matches(isDisplayed()))
        
        // Verify permission placeholder is NOT visible
        onView(withId(R.id.permission_placeholder))
            .check(matches(not(isDisplayed())))
    }
    
    @Test
    fun testStartMonitoringButton() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Verify start button is enabled
        onView(withId(R.id.btn_start_monitoring))
            .check(matches(isEnabled()))
        
        // Verify stop button is disabled
        onView(withId(R.id.btn_stop_monitoring))
            .check(matches(not(isEnabled())))
        
        // Click start monitoring
        onView(withId(R.id.btn_start_monitoring))
            .perform(click())
        
        // Wait a moment for camera to bind
        Thread.sleep(1000)
        
        // Verify start button is now disabled
        onView(withId(R.id.btn_start_monitoring))
            .check(matches(not(isEnabled())))
        
        // Verify stop button is now enabled
        onView(withId(R.id.btn_stop_monitoring))
            .check(matches(isEnabled()))
    }
    
    @Test
    fun testStopMonitoringButton() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Start monitoring first
        onView(withId(R.id.btn_start_monitoring))
            .perform(click())
        
        // Wait for camera to bind
        Thread.sleep(1000)
        
        // Click stop monitoring
        onView(withId(R.id.btn_stop_monitoring))
            .perform(click())
        
        // Verify start button is enabled again
        onView(withId(R.id.btn_start_monitoring))
            .check(matches(isEnabled()))
        
        // Verify stop button is disabled again
        onView(withId(R.id.btn_stop_monitoring))
            .check(matches(not(isEnabled())))
    }
    
    @Test
    fun testMultipleStartStopCycles() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Cycle 1
        onView(withId(R.id.btn_start_monitoring))
            .perform(click())
        Thread.sleep(500)
        onView(withId(R.id.btn_stop_monitoring))
            .perform(click())
        Thread.sleep(500)
        
        // Cycle 2
        onView(withId(R.id.btn_start_monitoring))
            .perform(click())
        Thread.sleep(500)
        onView(withId(R.id.btn_stop_monitoring))
            .perform(click())
        Thread.sleep(500)
        
        // Verify no crashes and buttons are in correct state
        onView(withId(R.id.btn_start_monitoring))
            .check(matches(isEnabled()))
        onView(withId(R.id.btn_stop_monitoring))
            .check(matches(not(isEnabled())))
    }
    
    @Test
    fun testOverlayViewIsVisible() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Start monitoring
        onView(withId(R.id.btn_start_monitoring))
            .perform(click())
        
        // Wait for camera and FPS updates
        Thread.sleep(2000)
        
        // Verify overlay view is still visible and displaying
        onView(withId(R.id.overlay_view))
            .check(matches(isDisplayed()))
        
        // Note: We can't easily verify FPS value > 0 without custom matchers,
        // but the overlay should be updating via Choreographer
    }
    
    @Test
    fun testCameraPreviewAttached() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Start monitoring
        onView(withId(R.id.btn_start_monitoring))
            .perform(click())
        
        // Wait for camera to bind
        Thread.sleep(1500)
        
        // Verify preview view is displayed and attached
        onView(withId(R.id.preview_view))
            .check(matches(isDisplayed()))
            .check(matches(isCompletelyDisplayed()))
    }
}

