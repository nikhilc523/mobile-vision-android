package edu.cs663.falldetect.data

/**
 * Data class representing a posture analysis result from Gemini.
 */
data class PostureAnalysis(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val score: Int, // 0-100
    val status: PostureStatus,
    val issues: List<String>,
    val recommendations: List<String>,
    val neckAngle: Float,
    val spineAngle: Float,
    val shoulderAlignment: Float
) {
    enum class PostureStatus {
        EXCELLENT,  // 90-100
        GOOD,       // 75-89
        FAIR,       // 60-74
        POOR        // 0-59
    }
    
    companion object {
        fun getStatusFromScore(score: Int): PostureStatus {
            return when {
                score >= 90 -> PostureStatus.EXCELLENT
                score >= 75 -> PostureStatus.GOOD
                score >= 60 -> PostureStatus.FAIR
                else -> PostureStatus.POOR
            }
        }
        
        fun getStatusEmoji(status: PostureStatus): String {
            return when (status) {
                PostureStatus.EXCELLENT -> "✅"
                PostureStatus.GOOD -> "🟢"
                PostureStatus.FAIR -> "⚠️"
                PostureStatus.POOR -> "🔴"
            }
        }
        
        fun getStatusColor(status: PostureStatus): Int {
            return when (status) {
                PostureStatus.EXCELLENT -> android.R.color.holo_green_dark
                PostureStatus.GOOD -> android.R.color.holo_green_light
                PostureStatus.FAIR -> android.R.color.holo_orange_light
                PostureStatus.POOR -> android.R.color.holo_red_dark
            }
        }
    }
}

