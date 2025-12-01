package edu.cs663.falldetect.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Choreographer
import android.view.View
import androidx.core.content.ContextCompat
import edu.cs663.falldetect.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom view for drawing time, FPS, and pose skeleton overlay on camera preview.
 * Displays a Material Design chip in the top-right corner with current time and FPS.
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Pre-allocated objects to avoid allocations in onDraw
    private val chipRect = RectF()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // Paint objects (pre-allocated)
    private val chipBackgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val chipTextPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
    }

    private val skeletonPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pose_skeleton)
        strokeWidth = 4f * context.resources.displayMetrics.density
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val jointPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.pose_joint)
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    // State
    private var currentFps = 0f
    private var clockEnabled = true
    private var fpsEnabled = true
    private var poseKeypoints: FloatArray? = null

    // Choreographer for smooth invalidation
    private val choreographer = Choreographer.getInstance()
    private var isInvalidating = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            invalidate()
            if (isInvalidating) {
                choreographer.postFrameCallbackDelayed(this, INVALIDATE_DELAY_MS)
            }
        }
    }

    init {
        updateThemeColors()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startInvalidating()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopInvalidating()
    }

    /**
     * Update FPS display.
     * Thread-safe - can be called from any thread.
     */
    fun setFps(fps: Float) {
        currentFps = fps
        // Don't call invalidate here - choreographer handles it
    }

    /**
     * Enable or disable clock display.
     */
    fun setClockEnabled(enabled: Boolean) {
        clockEnabled = enabled
    }

    /**
     * Enable or disable FPS display.
     */
    fun setFpsEnabled(enabled: Boolean) {
        fpsEnabled = enabled
    }

    /**
     * Update pose keypoints for drawing.
     *
     * @param keypoints Array of [x, y, confidence] for each joint
     */
    fun updatePose(keypoints: FloatArray?) {
        poseKeypoints = keypoints
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw time + FPS chip in top-right corner
        if (clockEnabled || fpsEnabled) {
            drawStatusChip(canvas)
        }

        // Draw pose skeleton when keypoints are available
        poseKeypoints?.let { keypoints ->
            drawPoseSkeleton(canvas, keypoints)
        }
    }

    /**
     * Draw the status chip with time and FPS in the top-right corner.
     */
    private fun drawStatusChip(canvas: Canvas) {
        val density = resources.displayMetrics.density
        val padding = 8f * density
        val cornerRadius = 12f * density
        val margin = 16f * density

        // Build the text to display
        val text = buildString {
            if (clockEnabled) {
                append(timeFormat.format(Date()))
            }
            if (clockEnabled && fpsEnabled) {
                append(" | ")
            }
            if (fpsEnabled) {
                append(String.format(Locale.US, "%.1f FPS", currentFps))
            }
        }

        // Measure text
        val textSize = 14f * density
        chipTextPaint.textSize = textSize
        val textWidth = chipTextPaint.measureText(text)
        val textHeight = chipTextPaint.fontMetrics.let { it.descent - it.ascent }

        // Calculate chip dimensions
        val chipWidth = textWidth + (padding * 2)
        val chipHeight = textHeight + (padding * 2)

        // Position chip in top-right corner
        val left = width - chipWidth - margin
        val top = margin
        val right = width - margin
        val bottom = top + chipHeight

        chipRect.set(left, top, right, bottom)

        // Draw chip background
        canvas.drawRoundRect(chipRect, cornerRadius, cornerRadius, chipBackgroundPaint)

        // Draw text
        val textX = left + padding
        val textY = top + padding - chipTextPaint.fontMetrics.ascent
        canvas.drawText(text, textX, textY, chipTextPaint)

        // Update content description for accessibility
        contentDescription = "Status: $text"
    }

    /**
     * Draw pose skeleton from keypoints.
     * TODO: Implement actual skeleton drawing logic when pose model is integrated.
     */
    private fun drawPoseSkeleton(canvas: Canvas, keypoints: FloatArray) {
        // Placeholder for skeleton drawing
        // Will draw lines between connected joints and circles at joint positions
        // Format: keypoints[i*3], keypoints[i*3+1], keypoints[i*3+2] = x, y, confidence
    }

    /**
     * Update theme colors based on current configuration.
     */
    private fun updateThemeColors() {
        val isDarkMode = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // Get Material color for chip background
        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            com.google.android.material.R.attr.colorSurfaceVariant,
            typedValue,
            true
        )

        val surfaceColor = if (typedValue.data != 0) {
            typedValue.data
        } else {
            // Fallback color
            if (isDarkMode) 0xFF2C2C2C.toInt() else 0xFFE0E0E0.toInt()
        }

        // Apply alpha for semi-transparency
        chipBackgroundPaint.color = (surfaceColor and 0x00FFFFFF) or 0x99000000.toInt()

        // Text color
        chipTextPaint.color = if (isDarkMode) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        updateThemeColors()
    }

    /**
     * Start choreographer-based invalidation at ~10-15 FPS.
     */
    private fun startInvalidating() {
        if (!isInvalidating) {
            isInvalidating = true
            choreographer.postFrameCallback(frameCallback)
        }
    }

    /**
     * Stop choreographer-based invalidation.
     */
    private fun stopInvalidating() {
        isInvalidating = false
        choreographer.removeFrameCallback(frameCallback)
    }

    companion object {
        private const val INVALIDATE_DELAY_MS = 100L // ~10 FPS for overlay updates
    }
}

