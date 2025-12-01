package edu.cs663.falldetect.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs663.falldetect.data.PostureAnalysis
import edu.cs663.falldetect.databinding.ItemPostureAnalysisBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying posture analyses in a RecyclerView.
 */
class PostureAdapter : ListAdapter<PostureAnalysis, PostureAdapter.PostureViewHolder>(PostureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostureViewHolder {
        val binding = ItemPostureAnalysisBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PostureViewHolder(
        private val binding: ItemPostureAnalysisBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(analysis: PostureAnalysis) {
            // Set status emoji
            binding.tvStatusEmoji.text = PostureAnalysis.getStatusEmoji(analysis.status)

            // Set score
            binding.tvScore.text = "Score: ${analysis.score}/100"
            binding.tvScore.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    PostureAnalysis.getStatusColor(analysis.status)
                )
            )

            // Set status text
            binding.tvStatus.text = when (analysis.status) {
                PostureAnalysis.PostureStatus.EXCELLENT -> "Excellent Posture"
                PostureAnalysis.PostureStatus.GOOD -> "Good Posture"
                PostureAnalysis.PostureStatus.FAIR -> "Fair Posture"
                PostureAnalysis.PostureStatus.POOR -> "Poor Posture"
            }

            // Set timestamp
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            binding.tvTimestamp.text = timeFormat.format(Date(analysis.timestamp))

            // Set issues
            if (analysis.issues.isNotEmpty()) {
                binding.tvIssues.text = analysis.issues.joinToString("\n") { "• $it" }
            } else {
                binding.tvIssues.text = "• No issues detected"
            }

            // Set recommendations
            if (analysis.recommendations.isNotEmpty()) {
                binding.tvRecommendations.text = analysis.recommendations.mapIndexed { index, rec ->
                    "${index + 1}. $rec"
                }.joinToString("\n")
            } else {
                binding.tvRecommendations.text = "Keep up the good work!"
            }

            // Set angles
            binding.tvAngles.text = "Neck: ${analysis.neckAngle.toInt()}° | " +
                    "Spine: ${analysis.spineAngle.toInt()}° | " +
                    "Shoulders: ${analysis.shoulderAlignment.toInt()}°"
        }
    }

    class PostureDiffCallback : DiffUtil.ItemCallback<PostureAnalysis>() {
        override fun areItemsTheSame(oldItem: PostureAnalysis, newItem: PostureAnalysis): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PostureAnalysis, newItem: PostureAnalysis): Boolean {
            return oldItem == newItem
        }
    }
}

