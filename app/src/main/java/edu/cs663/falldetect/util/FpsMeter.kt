package edu.cs663.falldetect.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicLong

/**
 * FPS meter for tracking frame rate.
 * Thread-safe implementation using atomic operations.
 */
class FpsMeter {
    
    private val frameCount = AtomicLong(0)
    private var currentFps = 0f
    private var lastUpdateTime = System.currentTimeMillis()
    
    private val handler = Handler(Looper.getMainLooper())
    private var updateCallback: ((Float) -> Unit)? = null
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateFps()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }
    
    /**
     * Record a frame tick.
     * Call this from the analyzer thread for each processed frame.
     */
    fun tick() {
        frameCount.incrementAndGet()
    }
    
    /**
     * Get the current FPS value.
     * Updated approximately once per second.
     */
    fun fps(): Float = currentFps
    
    /**
     * Start automatic FPS updates with a callback.
     * The callback will be invoked on the main thread.
     */
    fun startUpdates(callback: (Float) -> Unit) {
        updateCallback = callback
        handler.post(updateRunnable)
    }
    
    /**
     * Stop automatic FPS updates.
     */
    fun stopUpdates() {
        handler.removeCallbacks(updateRunnable)
        updateCallback = null
    }
    
    /**
     * Reset the FPS counter.
     */
    fun reset() {
        frameCount.set(0)
        currentFps = 0f
        lastUpdateTime = System.currentTimeMillis()
    }
    
    private fun updateFps() {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastUpdateTime
        
        if (elapsedTime > 0) {
            val frames = frameCount.getAndSet(0)
            currentFps = (frames * 1000f) / elapsedTime
            lastUpdateTime = currentTime
            
            updateCallback?.invoke(currentFps)
        }
    }
    
    companion object {
        private const val UPDATE_INTERVAL_MS = 1000L // Update every 1 second
    }
}

