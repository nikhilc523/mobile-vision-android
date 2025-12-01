package edu.cs663.falldetect.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import edu.cs663.falldetect.data.PostureAnalysis
import edu.cs663.falldetect.databinding.FragmentPostureBinding
import edu.cs663.falldetect.util.Log

/**
 * Fragment for viewing posture analysis history.
 */
class PostureFragment : Fragment() {

    private var _binding: FragmentPostureBinding? = null
    private val binding get() = _binding!!

    private lateinit var postureAdapter: PostureAdapter
    private val analyses = mutableListOf<PostureAnalysis>()
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadAnalyses()
    }

    private fun setupRecyclerView() {
        postureAdapter = PostureAdapter()
        binding.rvPostureAnalyses.adapter = postureAdapter
    }

    private fun loadAnalyses() {
        // Load analyses from SharedPreferences
        val prefs = requireContext().getSharedPreferences("posture_analyses", Context.MODE_PRIVATE)
        val analysesJson = prefs.getString("analyses_list", null)

        Log.d("Loading posture analyses from SharedPreferences")
        Log.d("JSON length: ${analysesJson?.length ?: 0}")

        analyses.clear()
        if (analysesJson != null) {
            try {
                val type = object : TypeToken<List<PostureAnalysis>>() {}.type
                val loadedAnalyses: List<PostureAnalysis> = gson.fromJson(analysesJson, type)
                analyses.addAll(loadedAnalyses)
                Log.i("Loaded ${analyses.size} posture analyses")
            } catch (e: Exception) {
                Log.e("Failed to load posture analyses: ${e.message}", e)
            }
        } else {
            Log.w("No posture analyses found in SharedPreferences")
        }

        // Sort by timestamp (newest first)
        analyses.sortByDescending { it.timestamp }

        // Update UI
        updateUI()
    }

    private fun updateUI() {
        if (analyses.isEmpty()) {
            binding.emptyState.isVisible = true
            binding.rvPostureAnalyses.isVisible = false
            binding.cardSummary.isVisible = false
            binding.tvDetailsHeader.isVisible = false
        } else {
            binding.emptyState.isVisible = false
            binding.rvPostureAnalyses.isVisible = true
            binding.cardSummary.isVisible = true
            binding.tvDetailsHeader.isVisible = true

            // Update summary
            updateSummary()

            // Update list
            postureAdapter.submitList(analyses.toList())
        }
    }

    private fun updateSummary() {
        if (analyses.isEmpty()) return

        // Calculate average score
        val avgScore = analyses.map { it.score }.average().toInt()
        binding.tvAvgScore.text = avgScore.toString()

        // Total checks
        binding.tvTotalChecks.text = analyses.size.toString()

        // Good posture percentage (score >= 75)
        val goodCount = analyses.count { it.score >= 75 }
        val goodPercent = (goodCount * 100 / analyses.size)
        binding.tvGoodPosturePercent.text = "$goodPercent%"
    }

    override fun onResume() {
        super.onResume()
        // Reload analyses when fragment becomes visible
        loadAnalyses()
        Log.d("PostureFragment resumed, loaded ${analyses.size} analyses")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

