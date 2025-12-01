package edu.cs663.falldetect.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import edu.cs663.falldetect.data.MonitoringSession
import edu.cs663.falldetect.databinding.FragmentLogsBinding
import edu.cs663.falldetect.util.Log

/**
 * Logs fragment for viewing fall detection history.
 */
class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionAdapter: SessionAdapter
    private val sessions = mutableListOf<MonitoringSession>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadSessions()
    }

    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter()
        binding.rvSessions.adapter = sessionAdapter
    }

    private fun loadSessions() {
        // Load sessions from SharedPreferences
        val prefs = requireContext().getSharedPreferences("monitoring_sessions", Context.MODE_PRIVATE)
        val sessionCount = prefs.getInt("session_count", 0)

        sessions.clear()
        for (i in 0 until sessionCount) {
            val startTime = prefs.getLong("session_${i}_start", 0)
            val endTime = prefs.getLong("session_${i}_end", 0)
            val fallCount = prefs.getInt("session_${i}_falls", 0)

            if (startTime > 0 && endTime > 0) {
                sessions.add(MonitoringSession(
                    id = i.toLong(),
                    startTime = startTime,
                    endTime = endTime,
                    fallCount = fallCount
                ))
            }
        }

        // Sort by start time (newest first)
        sessions.sortByDescending { it.startTime }

        // Update UI
        updateUI()
    }

    private fun updateUI() {
        if (sessions.isEmpty()) {
            binding.emptyState.isVisible = true
            binding.rvSessions.isVisible = false
        } else {
            binding.emptyState.isVisible = false
            binding.rvSessions.isVisible = true
            sessionAdapter.submitList(sessions.toList())
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload sessions when fragment becomes visible
        loadSessions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
