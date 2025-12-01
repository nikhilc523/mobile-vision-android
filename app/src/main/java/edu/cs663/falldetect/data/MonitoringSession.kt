package edu.cs663.falldetect.data

/**
 * Data class representing a monitoring session.
 */
data class MonitoringSession(
    val id: Long = System.currentTimeMillis(),
    val startTime: Long,
    val endTime: Long,
    val fallCount: Int = 0
) {
    /**
     * Get duration in milliseconds.
     */
    fun getDurationMs(): Long = endTime - startTime
    
    /**
     * Check if session had any falls.
     */
    fun hasFalls(): Boolean = fallCount > 0
}

