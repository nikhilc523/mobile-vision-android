package edu.cs663.falldetect.ui

import android.os.Bundle
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import edu.cs663.falldetect.R
import edu.cs663.falldetect.databinding.FragmentSettingsBinding
import edu.cs663.falldetect.util.EmergencyContact
import edu.cs663.falldetect.util.Log
import edu.cs663.falldetect.util.PrefsHelper
import java.util.Locale

/**
 * Settings fragment for configuring fall detection parameters.
 * Manages emergency contacts, countdown timer, and SMS/GPS alert settings.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var contactAdapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefsHelper = PrefsHelper(requireContext())

        setupContactsList()
        setupTimerSlider()
        setupSwitches()
        loadSettings()

        // Animate SMS settings card fade-in
        animateCardFadeIn()
    }

    private fun animateCardFadeIn() {
        binding.cardSmsSettings.alpha = 0f
        binding.cardSmsSettings.animate()
            .alpha(1f)
            .setDuration(300L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setupContactsList() {
        // Initialize adapter with delete callback
        contactAdapter = ContactAdapter { contact, position ->
            showDeleteContactDialog(contact, position)
        }

        // Setup RecyclerView
        binding.recyclerContacts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = contactAdapter
        }

        // Add contact button
        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun setupTimerSlider() {
        val saved = prefsHelper.getTimerDuration().coerceIn(10, 30)
        binding.sliderTimerDuration.value = saved.toFloat()
        updateCountdownChip(saved, animate = false)

        // Track last announced value for haptics
        var lastAnnounced = saved

        // Haptic + animated chip on value change
        binding.sliderTimerDuration.addOnChangeListener { _, value, fromUser ->
            val sec = value.toInt().coerceIn(10, 30)
            updateCountdownChip(sec, animate = fromUser)

            if (fromUser && sec != lastAnnounced) {
                // Subtle haptic for scrub
                binding.root.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                lastAnnounced = sec
            }
        }

        // Persist on touch release for fewer writes
        binding.sliderTimerDuration.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                val sec = slider.value.toInt().coerceIn(10, 30)
                prefsHelper.saveTimerDuration(sec)
                // Confirm haptic
                binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                // Accessibility announce
                binding.textTimerValue.announceForAccessibility("$sec seconds selected")
            }
        })
    }

    private fun updateCountdownChip(sec: Int, animate: Boolean) {
        binding.textTimerValue.text = String.format(Locale.US, "%d s", sec)

        if (animate) {
            // Subtle pop animation
            binding.textTimerValue.animate().cancel()
            binding.textTimerValue.scaleX = 0.92f
            binding.textTimerValue.scaleY = 0.92f
            binding.textTimerValue.alpha = 0.9f
            binding.textTimerValue.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(140L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupSwitches() {
        // SMS Alerts Switch
        binding.switchSmsEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.saveSmsEnabled(isChecked)

            // Haptic feedback
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            // User feedback via Snackbar
            val message = if (isChecked) {
                getString(R.string.sms_alerts_enabled)
            } else {
                getString(R.string.sms_alerts_disabled)
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

            // Accessibility announcement
            binding.switchSmsEnabled.announceForAccessibility(message)
        }

        // GPS Location Switch
        binding.switchGpsEnabled.setOnCheckedChangeListener { _, isChecked ->
            prefsHelper.saveGpsEnabled(isChecked)

            // Haptic feedback
            binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            // User feedback via Snackbar
            val message = if (isChecked) {
                getString(R.string.gps_location_enabled)
            } else {
                getString(R.string.gps_location_disabled)
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

            // Accessibility announcement
            binding.switchGpsEnabled.announceForAccessibility(message)
        }
    }

    private fun loadSettings() {
        // Load contacts
        refreshContactsList()

        // Timer duration is loaded in setupTimerSlider()

        // Load switches
        binding.switchSmsEnabled.isChecked = prefsHelper.isSmsEnabled()
        binding.switchGpsEnabled.isChecked = prefsHelper.isGpsEnabled()
    }

    private fun refreshContactsList() {
        val contacts = prefsHelper.getContacts()
        contactAdapter.submitList(contacts)
    }

    private fun showAddContactDialog() {
        // Check if max contacts reached
        if (prefsHelper.getContacts().size >= PrefsHelper.MAX_CONTACTS) {
            Snackbar.make(
                binding.root,
                R.string.error_max_contacts,
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        // Create dialog layout
        val dialogView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val nameInput = EditText(requireContext()).apply {
            hint = getString(R.string.dialog_contact_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        val phoneInput = EditText(requireContext()).apply {
            hint = getString(R.string.dialog_contact_phone_hint)
            inputType = InputType.TYPE_CLASS_PHONE
        }

        dialogView.addView(nameInput)
        dialogView.addView(phoneInput)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_add_contact_title)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_add) { _, _ ->
                val name = nameInput.text.toString().trim()
                val phone = phoneInput.text.toString().trim()

                if (validateContact(name, phone)) {
                    val contact = EmergencyContact(name, phone)
                    if (prefsHelper.addContact(contact)) {
                        refreshContactsList()
                        Snackbar.make(
                            binding.root,
                            R.string.contact_added,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        Log.i("Added contact: $name")
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showDeleteContactDialog(contact: EmergencyContact, index: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_contact_title)
            .setMessage(getString(R.string.dialog_delete_contact_message, contact.name))
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                prefsHelper.removeContact(index)
                refreshContactsList()
                Snackbar.make(
                    binding.root,
                    R.string.contact_removed,
                    Snackbar.LENGTH_SHORT
                ).show()
                Log.i("Removed contact: ${contact.name}")
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun validateContact(name: String, phone: String): Boolean {
        if (name.isEmpty()) {
            Snackbar.make(
                binding.root,
                R.string.error_name_required,
                Snackbar.LENGTH_LONG
            ).show()
            return false
        }

        if (phone.isEmpty()) {
            Snackbar.make(
                binding.root,
                R.string.error_phone_required,
                Snackbar.LENGTH_LONG
            ).show()
            return false
        }

        // Validate phone: digits only, max 15 characters
        if (!phone.matches(Regex("^[0-9+\\-() ]{1,20}$"))) {
            Snackbar.make(
                binding.root,
                R.string.error_phone_invalid,
                Snackbar.LENGTH_LONG
            ).show()
            return false
        }

        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
