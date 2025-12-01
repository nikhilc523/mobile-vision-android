package edu.cs663.falldetect.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper class for managing app settings via SharedPreferences.
 * Handles contact list serialization and settings persistence.
 */
class PrefsHelper(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Get the list of emergency contacts.
     */
    fun getContacts(): List<EmergencyContact> {
        val jsonString = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val contacts = mutableListOf<EmergencyContact>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                contacts.add(
                    EmergencyContact(
                        name = jsonObject.getString("name"),
                        phone = jsonObject.getString("phone")
                    )
                )
            }
            
            contacts
        } catch (e: Exception) {
            Log.e("Failed to parse contacts JSON", e)
            emptyList()
        }
    }
    
    /**
     * Save the list of emergency contacts.
     */
    fun saveContacts(contacts: List<EmergencyContact>) {
        try {
            val jsonArray = JSONArray()
            
            contacts.forEach { contact ->
                val jsonObject = JSONObject().apply {
                    put("name", contact.name)
                    put("phone", contact.phone)
                }
                jsonArray.put(jsonObject)
            }
            
            prefs.edit()
                .putString(KEY_CONTACTS, jsonArray.toString())
                .apply()
                
            Log.i("Saved ${contacts.size} contacts")
        } catch (e: Exception) {
            Log.e("Failed to save contacts", e)
        }
    }
    
    /**
     * Add a new emergency contact.
     * Returns true if successful, false if max contacts reached.
     */
    fun addContact(contact: EmergencyContact): Boolean {
        val currentContacts = getContacts().toMutableList()
        
        if (currentContacts.size >= MAX_CONTACTS) {
            return false
        }
        
        currentContacts.add(contact)
        saveContacts(currentContacts)
        return true
    }
    
    /**
     * Remove an emergency contact by index.
     */
    fun removeContact(index: Int) {
        val currentContacts = getContacts().toMutableList()
        
        if (index in currentContacts.indices) {
            currentContacts.removeAt(index)
            saveContacts(currentContacts)
        }
    }
    
    /**
     * Get the countdown timer duration in seconds.
     */
    fun getTimerDuration(): Int {
        return prefs.getInt(KEY_TIMER_DURATION, DEFAULT_TIMER_DURATION)
    }
    
    /**
     * Save the countdown timer duration in seconds.
     */
    fun saveTimerDuration(seconds: Int) {
        prefs.edit()
            .putInt(KEY_TIMER_DURATION, seconds)
            .apply()
        Log.i("Saved timer duration: $seconds seconds")
    }
    
    /**
     * Check if SMS alerts are enabled.
     */
    fun isSmsEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMS_ENABLED, DEFAULT_SMS_ENABLED)
    }
    
    /**
     * Save SMS alerts enabled state.
     */
    fun saveSmsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_SMS_ENABLED, enabled)
            .apply()
        Log.i("SMS alerts enabled: $enabled")
    }
    
    /**
     * Check if GPS location is enabled.
     */
    fun isGpsEnabled(): Boolean {
        return prefs.getBoolean(KEY_GPS_ENABLED, DEFAULT_GPS_ENABLED)
    }
    
    /**
     * Save GPS location enabled state.
     */
    fun saveGpsEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_GPS_ENABLED, enabled)
            .apply()
        Log.i("GPS location enabled: $enabled")
    }
    
    companion object {
        private const val PREFS_NAME = "fall_detect_prefs"
        
        // Keys
        private const val KEY_CONTACTS = "contacts"
        private const val KEY_TIMER_DURATION = "timer_duration_sec"
        private const val KEY_SMS_ENABLED = "sms_enabled"
        private const val KEY_GPS_ENABLED = "gps_enabled"
        
        // Defaults
        private const val DEFAULT_TIMER_DURATION = 15
        private const val DEFAULT_SMS_ENABLED = true
        private const val DEFAULT_GPS_ENABLED = true
        const val MAX_CONTACTS = 3
    }
}

/**
 * Data class representing an emergency contact.
 */
data class EmergencyContact(
    val name: String,
    val phone: String
)

