package edu.cs663.falldetect.ml

import edu.cs663.falldetect.data.PostureAnalysis
import edu.cs663.falldetect.util.Log

/**
 * Tracks posture status over time to detect sustained bad posture.
 * 
 * Key Concept:
 * - Short-term bad posture (< 30 seconds) = Normal activity (bending, reaching)
 * - Long-term bad posture (>= 30 seconds) = Actual problem that needs correction
 * 
 * Usage:
 * ```
 * val tracker = PostureTracker()
 * 
 * // Every 5 seconds, update with new analysis
 * val analysis = geminiAnalyzer.analyzePosture(keypoints)
 * val shouldSave = tracker.updatePosture(analysis)
 * 
 * if (shouldSave) {
 *     // Save to SharedPreferences and show warning
 *     savePostureAnalysis(analysis)
 * }
 * ```
 */
class PostureTracker(
    private val badPostureThreshold: Int = 70,  // Score below this is "bad"
    private val sustainedDurationSeconds: Int = 30  // Must be bad for this long to flag
) {
    
    // Current posture state
    private var currentStatus: PostureAnalysis.PostureStatus? = null
    private var currentStatusStartTime: Long = 0
    private var lastAnalysis: PostureAnalysis? = null
    
    // Tracking
    private var consecutiveBadPostureCount = 0
    private val checkIntervalSeconds = 5  // Posture checked every 5 seconds
    
    /**
     * Update posture tracker with new analysis.
     * 
     * @param analysis New posture analysis from Gemini
     * @return true if this analysis should be saved (sustained bad posture detected)
     */
    fun updatePosture(analysis: PostureAnalysis): Boolean {
        val now = System.currentTimeMillis()
        
        // Determine if this is "bad" posture
        val isBadPosture = analysis.score < badPostureThreshold
        
        // First analysis
        if (currentStatus == null) {
            currentStatus = analysis.status
            currentStatusStartTime = now
            lastAnalysis = analysis
            consecutiveBadPostureCount = if (isBadPosture) 1 else 0
            
            Log.d("PostureTracker initialized: status=${analysis.status}, score=${analysis.score}")
            return false  // Don't save first analysis
        }
        
        // Check if status changed
        val statusChanged = currentStatus != analysis.status
        
        if (statusChanged) {
            // Status changed - reset tracking
            Log.i("Posture status changed: ${currentStatus} -> ${analysis.status}")
            
            // Check if we should save the PREVIOUS sustained bad posture
            val shouldSavePrevious = shouldSavePosture()
            
            // Reset tracking
            currentStatus = analysis.status
            currentStatusStartTime = now
            consecutiveBadPostureCount = if (isBadPosture) 1 else 0
            lastAnalysis = analysis
            
            return shouldSavePrevious
        } else {
            // Status unchanged - continue tracking
            if (isBadPosture) {
                consecutiveBadPostureCount++
                
                val durationSeconds = (now - currentStatusStartTime) / 1000
                Log.d("Bad posture sustained: ${durationSeconds}s (count: $consecutiveBadPostureCount)")
                
                // Check if we've reached the threshold
                if (shouldSavePosture()) {
                    Log.w("⚠️ SUSTAINED BAD POSTURE DETECTED! Duration: ${durationSeconds}s, Score: ${analysis.score}")
                    
                    // Create analysis with duration info
                    val sustainedAnalysis = analysis.copy(
                        issues = analysis.issues + "⏱️ Sustained for ${durationSeconds}s"
                    )
                    lastAnalysis = sustainedAnalysis
                    
                    // Reset counter to avoid saving every 5 seconds
                    consecutiveBadPostureCount = 0
                    currentStatusStartTime = now
                    
                    return true
                }
            } else {
                // Good posture - reset bad posture counter
                consecutiveBadPostureCount = 0
            }
            
            lastAnalysis = analysis
            return false
        }
    }
    
    /**
     * Check if current posture should be saved.
     * Only save if bad posture has been sustained for the threshold duration.
     */
    private fun shouldSavePosture(): Boolean {
        val durationSeconds = (System.currentTimeMillis() - currentStatusStartTime) / 1000
        val requiredChecks = sustainedDurationSeconds / checkIntervalSeconds
        
        return consecutiveBadPostureCount >= requiredChecks && 
               durationSeconds >= sustainedDurationSeconds
    }
    
    /**
     * Get current posture duration in seconds.
     */
    fun getCurrentDuration(): Long {
        return (System.currentTimeMillis() - currentStatusStartTime) / 1000
    }
    
    /**
     * Get current posture status.
     */
    fun getCurrentStatus(): PostureAnalysis.PostureStatus? {
        return currentStatus
    }
    
    /**
     * Check if currently in bad posture.
     */
    fun isCurrentlyBadPosture(): Boolean {
        return lastAnalysis?.score?.let { it < badPostureThreshold } ?: false
    }
    
    /**
     * Reset tracker (e.g., when monitoring stops).
     */
    fun reset() {
        currentStatus = null
        currentStatusStartTime = 0
        lastAnalysis = null
        consecutiveBadPostureCount = 0
        Log.i("PostureTracker reset")
    }
    
    /**
     * Get summary of current tracking state.
     */
    fun getSummary(): String {
        val duration = getCurrentDuration()
        return "Status: $currentStatus, Duration: ${duration}s, Bad count: $consecutiveBadPostureCount"
    }
}

