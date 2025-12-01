package edu.cs663.falldetect.util

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner

/**
 * Unit tests for PrefsHelper.
 * Tests JSON serialization/deserialization for contact list and settings persistence.
 */
@RunWith(MockitoJUnitRunner::class)
class PrefsHelperTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var prefsHelper: PrefsHelper
    
    @Before
    fun setup() {
        // Setup mock behavior
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        
        `when`(mockSharedPreferences.edit())
            .thenReturn(mockEditor)
        
        `when`(mockEditor.putString(anyString(), anyString()))
            .thenReturn(mockEditor)
        
        `when`(mockEditor.putInt(anyString(), anyInt()))
            .thenReturn(mockEditor)
        
        `when`(mockEditor.putBoolean(anyString(), anyBoolean()))
            .thenReturn(mockEditor)
        
        prefsHelper = PrefsHelper(mockContext)
    }
    
    @Test
    fun testGetContactsEmpty() {
        `when`(mockSharedPreferences.getString("contacts", null))
            .thenReturn(null)
        
        val contacts = prefsHelper.getContacts()
        
        assertTrue("Contacts list should be empty", contacts.isEmpty())
    }
    
    @Test
    fun testGetContactsSingleContact() {
        val json = """[{"name":"John Doe","phone":"1234567890"}]"""
        
        `when`(mockSharedPreferences.getString("contacts", null))
            .thenReturn(json)
        
        val contacts = prefsHelper.getContacts()
        
        assertEquals("Should have 1 contact", 1, contacts.size)
        assertEquals("Name should match", "John Doe", contacts[0].name)
        assertEquals("Phone should match", "1234567890", contacts[0].phone)
    }
    
    @Test
    fun testGetContactsMultipleContacts() {
        val json = """[
            {"name":"Alice","phone":"1111111111"},
            {"name":"Bob","phone":"2222222222"},
            {"name":"Charlie","phone":"3333333333"}
        ]"""
        
        `when`(mockSharedPreferences.getString("contacts", null))
            .thenReturn(json)
        
        val contacts = prefsHelper.getContacts()
        
        assertEquals("Should have 3 contacts", 3, contacts.size)
        assertEquals("First contact name", "Alice", contacts[0].name)
        assertEquals("Second contact name", "Bob", contacts[1].name)
        assertEquals("Third contact name", "Charlie", contacts[2].name)
    }
    
    @Test
    fun testGetContactsInvalidJson() {
        `when`(mockSharedPreferences.getString("contacts", null))
            .thenReturn("invalid json")
        
        val contacts = prefsHelper.getContacts()
        
        assertTrue("Should return empty list for invalid JSON", contacts.isEmpty())
    }
    
    @Test
    fun testSaveContactsEmpty() {
        prefsHelper.saveContacts(emptyList())
        
        verify(mockEditor).putString(eq("contacts"), eq("[]"))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testSaveContactsSingle() {
        val contacts = listOf(
            EmergencyContact("John Doe", "1234567890")
        )
        
        prefsHelper.saveContacts(contacts)
        
        verify(mockEditor).putString(
            eq("contacts"),
            argThat { json ->
                json.contains("John Doe") && json.contains("1234567890")
            }
        )
        verify(mockEditor).apply()
    }
    
    @Test
    fun testSaveContactsMultiple() {
        val contacts = listOf(
            EmergencyContact("Alice", "1111111111"),
            EmergencyContact("Bob", "2222222222")
        )
        
        prefsHelper.saveContacts(contacts)
        
        verify(mockEditor).putString(
            eq("contacts"),
            argThat { json ->
                json.contains("Alice") && 
                json.contains("1111111111") &&
                json.contains("Bob") &&
                json.contains("2222222222")
            }
        )
        verify(mockEditor).apply()
    }
    
    @Test
    fun testAddContactSuccess() {
        `when`(mockSharedPreferences.getString("contacts", null))
            .thenReturn("[]")
        
        val contact = EmergencyContact("Test", "1234567890")
        val result = prefsHelper.addContact(contact)
        
        assertTrue("Should successfully add contact", result)
        verify(mockEditor).putString(anyString(), anyString())
        verify(mockEditor).apply()
    }
    
    @Test
    fun testAddContactMaxReached() {
        val json = """[
            {"name":"Alice","phone":"1111111111"},
            {"name":"Bob","phone":"2222222222"},
            {"name":"Charlie","phone":"3333333333"}
        ]"""
        
        `when`(mockSharedPreferences.getString("contacts", null))
            .thenReturn(json)
        
        val contact = EmergencyContact("David", "4444444444")
        val result = prefsHelper.addContact(contact)
        
        assertFalse("Should fail when max contacts reached", result)
        verify(mockEditor, never()).putString(anyString(), anyString())
    }
    
    @Test
    fun testRemoveContact() {
        val json = """[
            {"name":"Alice","phone":"1111111111"},
            {"name":"Bob","phone":"2222222222"}
        ]"""
        
        `when`(mockSharedPreferences.getString("contacts", null))
            .thenReturn(json)
        
        prefsHelper.removeContact(0)
        
        verify(mockEditor).putString(
            eq("contacts"),
            argThat { updatedJson ->
                !updatedJson.contains("Alice") && updatedJson.contains("Bob")
            }
        )
        verify(mockEditor).apply()
    }
    
    @Test
    fun testGetTimerDurationDefault() {
        `when`(mockSharedPreferences.getInt("timer_duration_sec", 15))
            .thenReturn(15)
        
        val duration = prefsHelper.getTimerDuration()
        
        assertEquals("Default timer duration should be 15", 15, duration)
    }
    
    @Test
    fun testSaveTimerDuration() {
        prefsHelper.saveTimerDuration(20)
        
        verify(mockEditor).putInt(eq("timer_duration_sec"), eq(20))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testIsSmsEnabledDefault() {
        `when`(mockSharedPreferences.getBoolean("sms_enabled", true))
            .thenReturn(true)
        
        val enabled = prefsHelper.isSmsEnabled()
        
        assertTrue("SMS should be enabled by default", enabled)
    }
    
    @Test
    fun testSaveSmsEnabled() {
        prefsHelper.saveSmsEnabled(false)
        
        verify(mockEditor).putBoolean(eq("sms_enabled"), eq(false))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testIsGpsEnabledDefault() {
        `when`(mockSharedPreferences.getBoolean("gps_enabled", true))
            .thenReturn(true)
        
        val enabled = prefsHelper.isGpsEnabled()
        
        assertTrue("GPS should be enabled by default", enabled)
    }
    
    @Test
    fun testSaveGpsEnabled() {
        prefsHelper.saveGpsEnabled(false)
        
        verify(mockEditor).putBoolean(eq("gps_enabled"), eq(false))
        verify(mockEditor).apply()
    }
    
    @Test
    fun testEmergencyContactDataClass() {
        val contact = EmergencyContact("Test Name", "1234567890")
        
        assertEquals("Name should match", "Test Name", contact.name)
        assertEquals("Phone should match", "1234567890", contact.phone)
    }
    
    @Test
    fun testEmergencyContactEquality() {
        val contact1 = EmergencyContact("Test", "123")
        val contact2 = EmergencyContact("Test", "123")
        val contact3 = EmergencyContact("Different", "456")
        
        assertEquals("Same contacts should be equal", contact1, contact2)
        assertNotEquals("Different contacts should not be equal", contact1, contact3)
    }
}

