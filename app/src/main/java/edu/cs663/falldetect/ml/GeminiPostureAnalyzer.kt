package edu.cs663.falldetect.ml

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import edu.cs663.falldetect.data.PostureAnalysis
import edu.cs663.falldetect.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Analyzes posture using Gemini API and body angle calculations.
 */
class GeminiPostureAnalyzer(context: Context) {
    
    companion object {
        private const val TAG = "GeminiPostureAnalyzer"
        
        // COCO keypoint indices
        private const val NOSE = 0
        private const val LEFT_SHOULDER = 5
        private const val RIGHT_SHOULDER = 6
        private const val LEFT_HIP = 11
        private const val RIGHT_HIP = 12
        
        // Ideal angle ranges
        private const val IDEAL_NECK_ANGLE_MIN = 165f
        private const val IDEAL_NECK_ANGLE_MAX = 175f
        private const val IDEAL_SPINE_ANGLE_MIN = 170f
        private const val IDEAL_SPINE_ANGLE_MAX = 180f
        private const val IDEAL_SHOULDER_ALIGNMENT_MAX = 5f
    }
    
    private val model: GenerativeModel
    
    init {
        // Get API key from resources or BuildConfig
        val apiKey = "AIzaSyCzLatfZs4ULYiRFFKvrb1NyQrMDxP7ubI"

        model = GenerativeModel(
            modelName = "gemini-2.0-flash-exp",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 500
            }
        )
        
        Log.d("GeminiPostureAnalyzer initialized", tag = TAG)
    }
    
    /**
     * Analyze posture from YOLO keypoints.
     * Keypoints format: [y0, x0, y1, x1, ..., y16, x16] (17 keypoints, 34 values)
     */
    suspend fun analyzePosture(keypoints: FloatArray): PostureAnalysis? = withContext(Dispatchers.IO) {
        try {
            // Calculate body angles
            val neckAngle = calculateNeckAngle(keypoints)
            val spineAngle = calculateSpineAngle(keypoints)
            val shoulderAlignment = calculateShoulderAlignment(keypoints)
            
            Log.d("Angles - Neck: $neckAngle°, Spine: $spineAngle°, Shoulder: $shoulderAlignment°", tag = TAG)
            
            // Determine posture state
            val postureState = determinePostureState(neckAngle, spineAngle, shoulderAlignment)
            
            // Call Gemini API for detailed analysis
            val geminiResult = callGeminiAPI(neckAngle, spineAngle, shoulderAlignment, postureState)
            
            geminiResult
        } catch (e: Exception) {
            Log.e("Failed to analyze posture: ${e.message}", e, tag = TAG)
            null
        }
    }
    
    private fun calculateNeckAngle(keypoints: FloatArray): Float {
        // Get keypoint coordinates (remember: [y, x] format)
        val noseY = keypoints[NOSE * 2]
        val noseX = keypoints[NOSE * 2 + 1]
        val leftShoulderY = keypoints[LEFT_SHOULDER * 2]
        val leftShoulderX = keypoints[LEFT_SHOULDER * 2 + 1]
        val rightShoulderY = keypoints[RIGHT_SHOULDER * 2]
        val rightShoulderX = keypoints[RIGHT_SHOULDER * 2 + 1]
        
        // Calculate neck midpoint (between shoulders)
        val neckY = (leftShoulderY + rightShoulderY) / 2
        val neckX = (leftShoulderX + rightShoulderX) / 2
        
        // Calculate angle between nose and neck relative to vertical
        val angle = calculateAngle(noseX, noseY, neckX, neckY, neckX, neckY + 0.1f)
        
        return angle
    }
    
    private fun calculateSpineAngle(keypoints: FloatArray): Float {
        // Get shoulder and hip coordinates
        val leftShoulderY = keypoints[LEFT_SHOULDER * 2]
        val leftShoulderX = keypoints[LEFT_SHOULDER * 2 + 1]
        val rightShoulderY = keypoints[RIGHT_SHOULDER * 2]
        val rightShoulderX = keypoints[RIGHT_SHOULDER * 2 + 1]
        val leftHipY = keypoints[LEFT_HIP * 2]
        val leftHipX = keypoints[LEFT_HIP * 2 + 1]
        val rightHipY = keypoints[RIGHT_HIP * 2]
        val rightHipX = keypoints[RIGHT_HIP * 2 + 1]
        
        // Calculate midpoints
        val shoulderMidY = (leftShoulderY + rightShoulderY) / 2
        val shoulderMidX = (leftShoulderX + rightShoulderX) / 2
        val hipMidY = (leftHipY + rightHipY) / 2
        val hipMidX = (leftHipX + rightHipX) / 2
        
        // Calculate angle relative to vertical
        val angle = calculateAngle(shoulderMidX, shoulderMidY, hipMidX, hipMidY, hipMidX, hipMidY + 0.1f)
        
        return angle
    }
    
    private fun calculateShoulderAlignment(keypoints: FloatArray): Float {
        // Get shoulder coordinates
        val leftShoulderY = keypoints[LEFT_SHOULDER * 2]
        val rightShoulderY = keypoints[RIGHT_SHOULDER * 2]
        
        // Calculate vertical difference (alignment)
        return kotlin.math.abs(leftShoulderY - rightShoulderY) * 100 // Convert to degrees approximation
    }
    
    private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        // Calculate angle between three points using atan2
        val angle1 = atan2((y1 - y2).toDouble(), (x1 - x2).toDouble())
        val angle2 = atan2((y3 - y2).toDouble(), (x3 - x2).toDouble())
        var angle = Math.toDegrees(angle2 - angle1)
        
        // Normalize to 0-180
        if (angle < 0) angle += 360
        if (angle > 180) angle = 360 - angle
        
        return angle.toFloat()
    }
    
    private fun determinePostureState(neckAngle: Float, spineAngle: Float, shoulderAlignment: Float): String {
        return when {
            neckAngle < 150 -> "forward_head"
            spineAngle < 160 -> "slouching"
            shoulderAlignment > 15 -> "uneven_shoulders"
            else -> "good_posture"
        }
    }
    
    private suspend fun callGeminiAPI(
        neckAngle: Float,
        spineAngle: Float,
        shoulderAlignment: Float,
        postureState: String
    ): PostureAnalysis {
        val prompt = """
            You are a posture analysis expert for elderly care.
            
            Current posture measurements:
            - Neck angle: ${neckAngle.toInt()}° (ideal: $IDEAL_NECK_ANGLE_MIN-$IDEAL_NECK_ANGLE_MAX°)
            - Spine angle: ${spineAngle.toInt()}° (ideal: $IDEAL_SPINE_ANGLE_MIN-$IDEAL_SPINE_ANGLE_MAX°)
            - Shoulder alignment: ${shoulderAlignment.toInt()}° (ideal: <$IDEAL_SHOULDER_ALIGNMENT_MAX°)
            - Detected state: $postureState
            
            Analyze this posture and provide:
            1. Overall posture score (0-100)
            2. List of specific issues (max 3, comma-separated)
            3. Actionable recommendations (max 3, numbered list)
            
            Keep recommendations simple and actionable for elderly users.
            Focus on immediate corrections they can make right now.
            
            Format your response EXACTLY as:
            SCORE: [number]
            ISSUES: [issue1, issue2, issue3]
            RECOMMENDATIONS:
            1. [recommendation1]
            2. [recommendation2]
            3. [recommendation3]
        """.trimIndent()
        
        try {
            Log.i("🤖 Calling Gemini API for posture analysis...", tag = TAG)
            val response = model.generateContent(prompt)
            val text = response.text ?: ""

            Log.i("✅ Gemini API SUCCESS! Response length: ${text.length}", tag = TAG)
            Log.d("Gemini response: $text", tag = TAG)

            val result = parseGeminiResponse(text, neckAngle, spineAngle, shoulderAlignment)
            Log.i("✅ Using GEMINI analysis: Score=${result.score}, Status=${result.status}", tag = TAG)
            return result
        } catch (e: Exception) {
            Log.e("❌ Gemini API FAILED: ${e.message}", e, tag = TAG)
            Log.w("⚠️ Using FALLBACK analysis instead", tag = TAG)
            // Return fallback analysis
            val fallback = createFallbackAnalysis(neckAngle, spineAngle, shoulderAlignment, postureState)
            Log.i("📊 Fallback analysis: Score=${fallback.score}, Status=${fallback.status}", tag = TAG)
            return fallback
        }
    }
    
    private fun parseGeminiResponse(
        text: String,
        neckAngle: Float,
        spineAngle: Float,
        shoulderAlignment: Float
    ): PostureAnalysis {
        var score = 75
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Parse score
        val scoreMatch = Regex("SCORE:\\s*(\\d+)").find(text)
        if (scoreMatch != null) {
            score = scoreMatch.groupValues[1].toIntOrNull() ?: 75
        }
        
        // Parse issues
        val issuesMatch = Regex("ISSUES:\\s*(.+?)(?=RECOMMENDATIONS:|$)", RegexOption.DOT_MATCHES_ALL).find(text)
        if (issuesMatch != null) {
            val issuesText = issuesMatch.groupValues[1].trim()
            issues.addAll(issuesText.split(",").map { it.trim() }.filter { it.isNotEmpty() })
        }
        
        // Parse recommendations
        val recommendationsMatch = Regex("RECOMMENDATIONS:\\s*(.+)", RegexOption.DOT_MATCHES_ALL).find(text)
        if (recommendationsMatch != null) {
            val recsText = recommendationsMatch.groupValues[1].trim()
            val recLines = recsText.split("\n").filter { it.matches(Regex("\\d+\\.\\s*.+")) }
            recommendations.addAll(recLines.map { it.replaceFirst(Regex("\\d+\\.\\s*"), "").trim() })
        }
        
        return PostureAnalysis(
            score = score.coerceIn(0, 100),
            status = PostureAnalysis.getStatusFromScore(score),
            issues = issues.take(3),
            recommendations = recommendations.take(3),
            neckAngle = neckAngle,
            spineAngle = spineAngle,
            shoulderAlignment = shoulderAlignment
        )
    }
    
    private fun createFallbackAnalysis(
        neckAngle: Float,
        spineAngle: Float,
        shoulderAlignment: Float,
        postureState: String
    ): PostureAnalysis {
        var score = 100
        val issues = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        // Calculate score based on angles
        if (neckAngle < IDEAL_NECK_ANGLE_MIN) {
            score -= ((IDEAL_NECK_ANGLE_MIN - neckAngle) / 2).toInt()
            issues.add("Forward head posture")
            recommendations.add("Lift your chin up and pull your head back")
        }
        
        if (spineAngle < IDEAL_SPINE_ANGLE_MIN) {
            score -= ((IDEAL_SPINE_ANGLE_MIN - spineAngle) / 2).toInt()
            issues.add("Slouching detected")
            recommendations.add("Straighten your back and sit upright")
        }
        
        if (shoulderAlignment > IDEAL_SHOULDER_ALIGNMENT_MAX) {
            score -= (shoulderAlignment - IDEAL_SHOULDER_ALIGNMENT_MAX).toInt()
            issues.add("Uneven shoulders")
            recommendations.add("Level your shoulders and relax")
        }
        
        if (issues.isEmpty()) {
            issues.add("Good posture")
            recommendations.add("Keep maintaining this posture!")
        }
        
        return PostureAnalysis(
            score = score.coerceIn(0, 100),
            status = PostureAnalysis.getStatusFromScore(score),
            issues = issues,
            recommendations = recommendations,
            neckAngle = neckAngle,
            spineAngle = spineAngle,
            shoulderAlignment = shoulderAlignment
        )
    }
}

