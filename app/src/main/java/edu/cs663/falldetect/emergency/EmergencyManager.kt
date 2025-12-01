package edu.cs663.falldetect.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import edu.cs663.falldetect.util.Log
import edu.cs663.falldetect.util.PrefsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages emergency countdown and SMS alert composition.
 * Does NOT send SMS directly - uses Intent to open SMS composer.
 * Includes TTS announcements, haptic feedback, and GPS location.
 */
class EmergencyManager(private val context: Context) {

    private var countdownTimer: CountDownTimer? = null
    private var isCountdownActive = false
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val vibrator: Vibrator? = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val prefsHelper = PrefsHelper(context)

    /**
     * Start emergency countdown with callback-based API.
     * Includes TTS announcements and haptic feedback.
     *
     * @param seconds Duration of countdown in seconds
     * @param onTick Called every second with remaining seconds
     * @param onCancel Called when countdown is cancelled (not used in this implementation)
     * @param onTimeout Called when countdown reaches zero
     */
    fun startCountdown(
        seconds: Int,
        onTick: (Int) -> Unit,
        onCancel: () -> Unit,
        onTimeout: () -> Unit
    ) {
        if (isCountdownActive) {
            Log.w("Countdown already active, ignoring start request")
            return
        }

        Log.i("Starting emergency countdown: $seconds seconds with TTS and haptics")
        isCountdownActive = true

        // Initialize TTS
        initializeTts()

        countdownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                Log.d("Countdown tick: $secondsRemaining seconds remaining")

                // Update UI
                onTick(secondsRemaining)

                // Haptic feedback every second
                vibrateOnce()

                // Voice reminders every 5 seconds
                if (secondsRemaining % 5 == 0 && secondsRemaining > 0) {
                    speak("Please tap I'm okay if you are fine.", TextToSpeech.QUEUE_ADD)
                }
            }

            override fun onFinish() {
                Log.i("Countdown finished - triggering emergency alert")
                isCountdownActive = false

                // Final announcement
                speak("No response detected. Preparing emergency message.", TextToSpeech.QUEUE_ADD)

                // Stronger haptic on timeout
                vibrateLong()

                // Compose and launch SMS
                composeEmergencySms()

                onTimeout()
            }
        }.start()
    }

    /**
     * Initialize Text-to-Speech engine.
     */
    private fun initializeTts() {
        if (tts != null) {
            Log.d("TTS already initialized")
            return
        }

        Log.i("Initializing TTS engine")
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.0f)
                isTtsReady = true

                Log.i("TTS initialized successfully")

                // Initial announcement
                speak("A fall is detected. Are you okay?", TextToSpeech.QUEUE_FLUSH)
            } else {
                Log.w("TTS initialization failed with status: $status")
                isTtsReady = false
            }
        }
    }

    /**
     * Speak a message using TTS.
     */
    private fun speak(message: String, queueMode: Int) {
        if (!isTtsReady) {
            Log.d("TTS not ready, skipping message: $message")
            return
        }

        Log.d("Speaking: $message")
        tts?.speak(message, queueMode, null, null)
    }

    /**
     * Vibrate once (short pulse).
     */
    private fun vibrateOnce() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (e: Exception) {
            Log.w("Vibration failed: ${e.message}")
        }
    }

    /**
     * Vibrate longer (for timeout/cancel).
     */
    private fun vibrateLong() {
        if (vibrator == null || !vibrator.hasVibrator()) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(300)
            }
        } catch (e: Exception) {
            Log.w("Vibration failed: ${e.message}")
        }
    }

    /**
     * Stop the countdown timer and cleanup resources.
     */
    fun stop() {
        if (!isCountdownActive) {
            Log.d("No active countdown to stop")
        }

        Log.i("Stopping emergency countdown and cleaning up")

        // Stop countdown
        countdownTimer?.cancel()
        countdownTimer = null
        isCountdownActive = false

        // Stop TTS
        tts?.stop()
        tts?.shutdown()
        tts = null
        isTtsReady = false

        // Haptic feedback on cancel
        vibrateLong()
    }

    /**
     * Cancel active countdown (alias for stop).
     */
    fun cancel() {
        stop()
    }

    /**
     * Compose and launch emergency SMS with GPS location.
     * Respects SMS/GPS settings and handles permissions gracefully.
     */
    private fun composeEmergencySms() {
        Log.i("Composing emergency SMS")

        // Check if SMS alerts are enabled
        if (!prefsHelper.isSmsEnabled()) {
            Log.i("SMS alerts disabled in settings, skipping")
            return
        }

        // Get emergency contacts
        val contacts = prefsHelper.getContacts()
        if (contacts.isEmpty()) {
            Log.w("No emergency contacts configured, cannot send SMS")
            return
        }

        // Check if GPS should be included
        val includeGps = prefsHelper.isGpsEnabled()

        if (includeGps) {
            // Try to get location and compose SMS
            fetchLocationAndComposeSms(contacts)
        } else {
            // Compose SMS without location
            Log.i("GPS disabled in settings, composing SMS without location")
            launchSmsComposer(contacts, "location disabled")
        }
    }

    /**
     * Fetch last known location and compose SMS.
     */
    private fun fetchLocationAndComposeSms(contacts: List<edu.cs663.falldetect.util.EmergencyContact>) {
        // Check location permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w("Location permission not granted, composing SMS without GPS")
            launchSmsComposer(contacts, "unknown (GPS permission missing)")
            return
        }

        try {
            Log.i("Fetching last known location")

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    val locationText = if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude
                        val accuracy = location.accuracy.toInt()
                        Log.i("Location obtained: $lat, $lon (±$accuracy m)")
                        String.format(Locale.US, "%.4f, %.4f (±%d m)", lat, lon, accuracy)
                    } else {
                        Log.w("Last known location is null")
                        "unknown"
                    }

                    launchSmsComposer(contacts, locationText)
                }
                .addOnFailureListener { exception ->
                    Log.w("Failed to get location: ${exception.message}")
                    launchSmsComposer(contacts, "unknown")
                }
        } catch (e: SecurityException) {
            Log.w("SecurityException when fetching location: ${e.message}")
            launchSmsComposer(contacts, "unknown (GPS permission missing)")
        }
    }

    /**
     * Launch SMS composer with emergency message.
     */
    private fun launchSmsComposer(
        contacts: List<edu.cs663.falldetect.util.EmergencyContact>,
        locationText: String
    ) {
        // Build recipient list (semicolon-separated for multiple recipients)
        val recipients = contacts.joinToString(";") { it.phone }

        // Build message
        val message = buildFormattedMessage(locationText)

        Log.i("Launching SMS composer for ${contacts.size} contact(s)")
        Log.d("Recipients: $recipients")
        Log.d("Message: $message")

        // Create SMS intent
        val smsUri = Uri.parse("smsto:$recipients")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(smsIntent)
            Log.i("SMS composer launched successfully")
        } catch (e: Exception) {
            Log.e("Failed to launch SMS composer: ${e.message}", e)
        }
    }

    /**
     * Build formatted emergency message with timestamp and location.
     */
    private fun buildFormattedMessage(locationText: String): String {
        // Get current time
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

        // User name (could be made configurable in settings)
        val userName = "User" // TODO: Make this configurable

        // Build Google Maps link if we have valid coordinates
        val mapLink = if (locationText.contains(",") && !locationText.startsWith("unknown")) {
            val coords = locationText.split(",")[0].trim() + "," +
                        locationText.split(",")[1].split("(")[0].trim()
            "\nMap: https://maps.google.com/?q=$coords"
        } else {
            ""
        }

        return """
            ALERT: Possible fall detected for $userName at $time.
            Location: $locationText$mapLink
            If you reach them, reply OK.
        """.trimIndent()
    }

    /**
     * Compose SMS intent with emergency message and location (legacy method).
     * Opens SMS app with pre-filled message.
     *
     * @param phoneNumber Emergency contact number
     * @param latitude Current latitude (or null if unavailable)
     * @param longitude Current longitude (or null if unavailable)
     */
    fun composeSmsIntent(
        phoneNumber: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Intent {
        val message = buildEmergencyMessage(latitude, longitude)

        Log.i("Composing SMS to $phoneNumber: $message")

        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun buildEmergencyMessage(lat: Double?, lon: Double?): String {
        val baseMessage = "FALL DETECTED! I may need assistance."

        return if (lat != null && lon != null) {
            val mapsUrl = "https://maps.google.com/?q=$lat,$lon"
            "$baseMessage\n\nMy location: $mapsUrl"
        } else {
            "$baseMessage\n\nLocation unavailable."
        }
    }
    
    /**
     * Check if countdown is currently active.
     */
    fun isActive(): Boolean = isCountdownActive
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        cancel()
    }
}

