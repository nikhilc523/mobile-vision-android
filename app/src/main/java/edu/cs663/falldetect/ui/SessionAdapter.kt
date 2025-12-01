package edu.cs663.falldetect.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cs663.falldetect.R
import edu.cs663.falldetect.data.MonitoringSession
import edu.cs663.falldetect.databinding.ItemMonitoringSessionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying monitoring sessions in a RecyclerView.
 */
class SessionAdapter : ListAdapter<MonitoringSession, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemMonitoringSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(
        private val binding: ItemMonitoringSessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: MonitoringSession) {
            // Set fall status
            if (session.hasFalls()) {
                binding.tvFallStatus.text = "${session.fallCount} Fall${if (session.fallCount > 1) "s" else ""} Detected"
                binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(
                    binding.root.context,
                    R.color.red_500
                )
            } else {
                binding.tvFallStatus.text = "No Falls"
                binding.statusIndicator.backgroundTintList = ContextCompat.getColorStateList(
                    binding.root.context,
                    R.color.green_500
                )
            }

            // Set date and time
            val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            binding.tvDateTime.text = dateFormat.format(Date(session.startTime))

            // Set duration
            binding.tvDuration.text = "Duration: ${formatDuration(session.getDurationMs())}"
        }

        private fun formatDuration(durationMs: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

            return when {
                hours > 0 -> "$hours hr $minutes min"
                minutes > 0 -> "$minutes min $seconds sec"
                else -> "$seconds sec"
            }
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<MonitoringSession>() {
        override fun areItemsTheSame(oldItem: MonitoringSession, newItem: MonitoringSession): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MonitoringSession, newItem: MonitoringSession): Boolean {
            return oldItem == newItem
        }
    }
}

