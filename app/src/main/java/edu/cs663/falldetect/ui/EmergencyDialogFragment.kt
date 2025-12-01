package edu.cs663.falldetect.ui

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import edu.cs663.falldetect.databinding.DialogEmergencyBinding
import edu.cs663.falldetect.emergency.EmergencyManager
import edu.cs663.falldetect.util.Log
import edu.cs663.falldetect.util.PrefsHelper
import java.util.Locale

/**
 * Full-screen emergency countdown dialog.
 * Shows animated countdown with progress ring, TTS announcements, haptic feedback.
 * On timeout, launches SMS composer with GPS location.
 *
 * Enhanced version with voice announcements, vibration, and emergency SMS.
 */
class EmergencyDialogFragment : DialogFragment() {

    private lateinit var binding: DialogEmergencyBinding
    private var emergencyManager: EmergencyManager? = null
    private var countdownSeconds = 15
    private val handler = Handler(Looper.getMainLooper())

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!fineLocationGranted && !coarseLocationGranted) {
            Log.w("Location permissions denied")
            Snackbar.make(
                binding.root,
                "Location permission denied — GPS link may be missing.",
                Snackbar.LENGTH_LONG
            ).show()
        } else {
            Log.i("Location permissions granted")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogEmergencyBinding.inflate(layoutInflater)
        
        // Create full-screen dialog
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(binding.root)
        dialog.setCancelable(false) // Prevent dismissal by back button
        
        return dialog
    }

    override fun onStart() {
        super.onStart()

        // Load countdown duration from preferences
        val prefsHelper = PrefsHelper(requireContext())
        countdownSeconds = prefsHelper.getTimerDuration()

        Log.i("Emergency dialog started with countdown: $countdownSeconds seconds")

        // Request location permissions if GPS is enabled
        if (prefsHelper.isGpsEnabled()) {
            requestLocationPermissions()
        }

        // Initialize emergency manager
        emergencyManager = EmergencyManager(requireContext())

        // Setup UI
        setupProgressIndicator()
        setupCancelButton()

        // Delay countdown start by 1 second for natural feel
        // This allows the dialog to fully appear before voice starts
        handler.postDelayed({
            startCountdown()
        }, 1000)
    }

    /**
     * Request location permissions for GPS functionality.
     */
    private fun requestLocationPermissions() {
        Log.i("Requesting location permissions")
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun setupProgressIndicator() {
        // Set max value for progress indicator
        binding.progressCountdown.max = countdownSeconds
        binding.progressCountdown.progress = countdownSeconds
        
        // Set initial countdown value
        binding.textCountdownValue.text = countdownSeconds.toString()
    }

    private fun setupCancelButton() {
        binding.btnCancel.setOnClickListener {
            Log.i("User cancelled emergency countdown")
            
            // Haptic feedback
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            
            // Stop countdown and dismiss
            emergencyManager?.stop()
            dismiss()
        }
    }

    private fun startCountdown() {
        Log.i("Starting countdown with TTS and haptics")

        emergencyManager?.startCountdown(
            seconds = countdownSeconds,
            onTick = { remaining ->
                // Update countdown value with "s" suffix
                binding.textCountdownValue.text = String.format(Locale.US, "%d", remaining)

                // Update progress ring
                binding.progressCountdown.progress = remaining

                // Animate countdown value (pulse effect)
                animateCountdownValue()

                Log.d("Countdown tick: $remaining seconds")
            },
            onCancel = {
                // Not used in this implementation
                Log.d("Countdown cancelled callback")
            },
            onTimeout = {
                Log.i("Countdown timeout - emergency alert triggered")

                // SMS composer will be launched by EmergencyManager
                // Dismiss dialog after a short delay to let TTS finish and SMS composer open
                handler.postDelayed({
                    dismiss()
                }, 2500)
            }
        )
    }

    private fun animateCountdownValue() {
        // Pulse animation: scale up and fade slightly, then back to normal
        binding.textCountdownValue.animate().cancel()
        
        binding.textCountdownValue.scaleX = 0.95f
        binding.textCountdownValue.scaleY = 0.95f
        binding.textCountdownValue.alpha = 0.85f
        
        binding.textCountdownValue.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(150L)
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Remove any pending callbacks
        handler.removeCallbacksAndMessages(null)

        // Stop countdown if still running (this also stops TTS and vibration)
        emergencyManager?.stop()
        emergencyManager = null

        Log.d("Emergency dialog destroyed")
    }
}

