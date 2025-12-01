package edu.cs663.falldetect

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.cs663.falldetect.ui.MainActivity
import edu.cs663.falldetect.util.PrefsHelper
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for SettingsFragment.
 * Tests contact management, timer slider, and toggle persistence.
 */
@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {
    
    private lateinit var context: Context
    private lateinit var prefsHelper: PrefsHelper
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        prefsHelper = PrefsHelper(context)
        
        // Clear all settings before each test
        clearAllSettings()
    }
    
    @After
    fun teardown() {
        // Clean up after tests
        clearAllSettings()
    }
    
    private fun clearAllSettings() {
        prefsHelper.saveContacts(emptyList())
        prefsHelper.saveTimerDuration(15)
        prefsHelper.saveSmsEnabled(true)
        prefsHelper.saveGpsEnabled(true)
    }
    
    @Test
    fun testNavigateToSettings() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        // Verify settings elements are displayed
        onView(withId(R.id.btn_add_contact))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.slider_timer_duration))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.switch_sms_enabled))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.switch_gps_enabled))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testTimerSliderPersistence() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        // Wait a moment for fragment to load
        Thread.sleep(500)
        
        // Verify initial value is 15s
        onView(withId(R.id.text_timer_value))
            .check(matches(withText(containsString("15"))))
        
        // Change slider value (this is tricky with Espresso, so we'll verify persistence via PrefsHelper)
        val newValue = 20
        prefsHelper.saveTimerDuration(newValue)
        
        // Restart activity
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify value persisted
        onView(withId(R.id.text_timer_value))
            .check(matches(withText(containsString("20"))))
    }
    
    @Test
    fun testSmsTogglePersistence() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify initial state is checked
        onView(withId(R.id.switch_sms_enabled))
            .check(matches(isChecked()))
        
        // Toggle off
        onView(withId(R.id.switch_sms_enabled))
            .perform(click())
        
        Thread.sleep(200)
        
        // Verify it's unchecked
        onView(withId(R.id.switch_sms_enabled))
            .check(matches(isNotChecked()))
        
        // Restart activity
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify state persisted
        onView(withId(R.id.switch_sms_enabled))
            .check(matches(isNotChecked()))
    }
    
    @Test
    fun testGpsTogglePersistence() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify initial state is checked
        onView(withId(R.id.switch_gps_enabled))
            .check(matches(isChecked()))
        
        // Toggle off
        onView(withId(R.id.switch_gps_enabled))
            .perform(click())
        
        Thread.sleep(200)
        
        // Verify it's unchecked
        onView(withId(R.id.switch_gps_enabled))
            .check(matches(isNotChecked()))
        
        // Restart activity
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify state persisted
        onView(withId(R.id.switch_gps_enabled))
            .check(matches(isNotChecked()))
    }
    
    @Test
    fun testAddContactButton() {
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Click add contact button
        onView(withId(R.id.btn_add_contact))
            .perform(click())
        
        // Verify dialog appears (check for dialog title)
        onView(withText(R.string.dialog_add_contact_title))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testContactPersistence() {
        // Add a contact via PrefsHelper
        prefsHelper.saveContacts(listOf(
            edu.cs663.falldetect.util.EmergencyContact("John Doe", "1234567890")
        ))
        
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify contact is displayed
        onView(withText("John Doe"))
            .check(matches(isDisplayed()))
        
        onView(withText("1234567890"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun testMultipleContactsPersistence() {
        // Add multiple contacts
        prefsHelper.saveContacts(listOf(
            edu.cs663.falldetect.util.EmergencyContact("Alice", "1111111111"),
            edu.cs663.falldetect.util.EmergencyContact("Bob", "2222222222")
        ))
        
        ActivityScenario.launch(MainActivity::class.java)
        
        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())
        
        Thread.sleep(500)
        
        // Verify both contacts are displayed
        onView(withText("Alice"))
            .check(matches(isDisplayed()))
        
        onView(withText("Bob"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testRecyclerViewDisplaysContacts() {
        // Add contacts via PrefsHelper
        prefsHelper.saveContacts(listOf(
            edu.cs663.falldetect.util.EmergencyContact("Test User 1", "5551234567"),
            edu.cs663.falldetect.util.EmergencyContact("Test User 2", "5559876543")
        ))

        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Verify RecyclerView is displayed
        onView(withId(R.id.recycler_contacts))
            .check(matches(isDisplayed()))

        // Verify contacts are in RecyclerView
        onView(withText("Test User 1"))
            .check(matches(isDisplayed()))

        onView(withText("5551234567"))
            .check(matches(isDisplayed()))

        onView(withText("Test User 2"))
            .check(matches(isDisplayed()))

        onView(withText("5559876543"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddContactViaDialog() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Click add contact button
        onView(withId(R.id.btn_add_contact))
            .perform(click())

        Thread.sleep(300)

        // Enter contact details
        onView(withHint(R.string.dialog_contact_name_hint))
            .perform(typeText("New Contact"), closeSoftKeyboard())

        onView(withHint(R.string.dialog_contact_phone_hint))
            .perform(typeText("5551112222"), closeSoftKeyboard())

        // Click Add button
        onView(withText(R.string.dialog_add))
            .perform(click())

        Thread.sleep(500)

        // Verify contact appears in list
        onView(withText("New Contact"))
            .check(matches(isDisplayed()))

        onView(withText("5551112222"))
            .check(matches(isDisplayed()))

        // Verify persistence
        val contacts = prefsHelper.getContacts()
        assert(contacts.size == 1)
        assert(contacts[0].name == "New Contact")
        assert(contacts[0].phone == "5551112222")
    }

    @Test
    fun testDeleteContactViaDialog() {
        // Add a contact first
        prefsHelper.saveContacts(listOf(
            edu.cs663.falldetect.util.EmergencyContact("Delete Me", "5559999999")
        ))

        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Verify contact is displayed
        onView(withText("Delete Me"))
            .check(matches(isDisplayed()))

        // Click delete button (find by content description)
        onView(withId(R.id.btn_delete_contact))
            .perform(click())

        Thread.sleep(300)

        // Confirm deletion
        onView(withText(R.string.dialog_delete))
            .perform(click())

        Thread.sleep(500)

        // Verify contact is removed
        val contacts = prefsHelper.getContacts()
        assert(contacts.isEmpty())
    }

    @Test
    fun testMaxContactsLimit() {
        // Add 3 contacts (max limit)
        prefsHelper.saveContacts(listOf(
            edu.cs663.falldetect.util.EmergencyContact("Contact 1", "1111111111"),
            edu.cs663.falldetect.util.EmergencyContact("Contact 2", "2222222222"),
            edu.cs663.falldetect.util.EmergencyContact("Contact 3", "3333333333")
        ))

        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Try to add another contact
        onView(withId(R.id.btn_add_contact))
            .perform(click())

        Thread.sleep(300)

        // Verify error message is shown (Snackbar)
        onView(withText(R.string.error_max_contacts))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testContactPersistenceAcrossFragmentRecreation() {
        // Add a contact
        prefsHelper.saveContacts(listOf(
            edu.cs663.falldetect.util.EmergencyContact("Persistent User", "5554443333")
        ))

        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Verify contact is displayed
        onView(withText("Persistent User"))
            .check(matches(isDisplayed()))

        // Navigate away to Home
        onView(withId(R.id.homeFragment))
            .perform(click())

        Thread.sleep(300)

        // Navigate back to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Verify contact is still displayed
        onView(withText("Persistent User"))
            .check(matches(isDisplayed()))

        onView(withText("5554443333"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCountdownTimerDefaultValue() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Verify default value is 15s
        onView(withId(R.id.text_timer_value))
            .check(matches(withText("15 s")))

        // Verify slider is at default position
        onView(withId(R.id.slider_timer_duration))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCountdownTimerBoundaryValues() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Test minimum value (10s)
        prefsHelper.saveTimerDuration(10)

        // Restart to reload
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.text_timer_value))
            .check(matches(withText("10 s")))

        // Test maximum value (30s)
        prefsHelper.saveTimerDuration(30)

        // Restart to reload
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        onView(withId(R.id.text_timer_value))
            .check(matches(withText("30 s")))
    }

    @Test
    fun testCountdownTimerOutOfRangeValuesClamped() {
        // Test value below minimum
        prefsHelper.saveTimerDuration(5)

        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Should be clamped to 10
        onView(withId(R.id.text_timer_value))
            .check(matches(withText("10 s")))

        // Test value above maximum
        prefsHelper.saveTimerDuration(99)

        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Should be clamped to 30
        onView(withId(R.id.text_timer_value))
            .check(matches(withText("30 s")))
    }

    @Test
    fun testCountdownTimerChipDisplayed() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Verify chip is displayed with proper styling
        onView(withId(R.id.text_timer_value))
            .check(matches(isDisplayed()))
            .check(matches(withText(containsString("s")))) // Contains "s" for seconds
    }

    @Test
    fun testSmsToggleShowsSnackbar() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Toggle SMS switch off
        onView(withId(R.id.switch_sms_enabled))
            .perform(click())

        Thread.sleep(200)

        // Verify Snackbar appears with correct message
        onView(withText("SMS alerts disabled"))
            .check(matches(isDisplayed()))

        Thread.sleep(2000) // Wait for Snackbar to disappear

        // Toggle SMS switch on
        onView(withId(R.id.switch_sms_enabled))
            .perform(click())

        Thread.sleep(200)

        // Verify Snackbar appears with correct message
        onView(withText("SMS alerts enabled"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testGpsToggleShowsSnackbar() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Toggle GPS switch off
        onView(withId(R.id.switch_gps_enabled))
            .perform(click())

        Thread.sleep(200)

        // Verify Snackbar appears with correct message
        onView(withText("GPS location excluded"))
            .check(matches(isDisplayed()))

        Thread.sleep(2000) // Wait for Snackbar to disappear

        // Toggle GPS switch on
        onView(withId(R.id.switch_gps_enabled))
            .perform(click())

        Thread.sleep(200)

        // Verify Snackbar appears with correct message
        onView(withText("GPS location included"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSmsSettingsCardDisplayed() {
        ActivityScenario.launch(MainActivity::class.java)

        // Navigate to Settings
        onView(withId(R.id.settingsFragment))
            .perform(click())

        Thread.sleep(500)

        // Verify SMS settings card is displayed
        onView(withId(R.id.card_sms_settings))
            .check(matches(isDisplayed()))

        // Verify section title
        onView(withText("SMS Alert Settings"))
            .check(matches(isDisplayed()))

        // Verify both switches are displayed
        onView(withId(R.id.switch_sms_enabled))
            .check(matches(isDisplayed()))

        onView(withId(R.id.switch_gps_enabled))
            .check(matches(isDisplayed()))
    }
}

