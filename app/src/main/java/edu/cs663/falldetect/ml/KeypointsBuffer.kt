package edu.cs663.falldetect.ml

import edu.cs663.falldetect.util.Log
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe sliding window buffer for keypoints.
 * 
 * Maintains a FIFO queue of the last N frames of keypoints for fall detection.
 * When the buffer is full (30 frames), it can be used for inference.
 * 
 * Usage:
 * ```
 * val buffer = KeypointsBuffer(windowSize = 30)
 * 
 * // Add frames as they come in
 * buffer.add(frame1Keypoints)  // 34 features
 * buffer.add(frame2Keypoints)
 * ...
 * 
 * // When buffer is full, run detection
 * if (buffer.isFull()) {
 *     val input = buffer.toFloatArray()  // 1020 values (30 × 34)
 *     val probability = fallDetector.detectFall(input)
 * }
 * ```
 * 
 * @param windowSize Number of frames to keep (default: 30 for 1 second @ 30 FPS)
 * @param featuresPerFrame Number of features per frame (default: 34 for 17 keypoints × 2 coords)
 */
class KeypointsBuffer(
    private val windowSize: Int = 30,
    private val featuresPerFrame: Int = 34
) {
    private val buffer = mutableListOf<FloatArray>()
    private val lock = ReentrantLock()
    
    /**
     * Add a new frame of keypoints to the buffer.
     * If buffer is full, removes the oldest frame (FIFO).
     * 
     * @param keypoints FloatArray of size 34 (17 keypoints × 2 coordinates [y, x])
     * @throws IllegalArgumentException if keypoints size is incorrect
     */
    fun add(keypoints: FloatArray) {
        require(keypoints.size == featuresPerFrame) {
            "Each frame must have $featuresPerFrame features (17 keypoints × 2 coords), got ${keypoints.size}"
        }
        
        lock.withLock {
            // Add new frame
            buffer.add(keypoints.copyOf())  // Copy to avoid external modifications
            
            // Remove oldest frame if buffer exceeds window size
            if (buffer.size > windowSize) {
                buffer.removeAt(0)
            }
        }
    }
    
    /**
     * Check if buffer is full (has windowSize frames).
     * 
     * @return true if buffer has exactly windowSize frames
     */
    fun isFull(): Boolean {
        lock.withLock {
            return buffer.size == windowSize
        }
    }
    
    /**
     * Get current number of frames in buffer.
     * 
     * @return Number of frames currently in buffer
     */
    fun size(): Int {
        lock.withLock {
            return buffer.size
        }
    }
    
    /**
     * Check if buffer is empty.
     * 
     * @return true if buffer has no frames
     */
    fun isEmpty(): Boolean {
        lock.withLock {
            return buffer.isEmpty()
        }
    }
    
    /**
     * Convert buffer to flat FloatArray for model input.
     * 
     * @return FloatArray of size (windowSize × featuresPerFrame)
     * @throws IllegalStateException if buffer is not full
     */
    fun toFloatArray(): FloatArray {
        lock.withLock {
            require(buffer.size == windowSize) {
                "Buffer must be full ($windowSize frames) before converting to array, current size: ${buffer.size}"
            }
            
            val result = FloatArray(windowSize * featuresPerFrame)
            for (i in 0 until windowSize) {
                System.arraycopy(buffer[i], 0, result, i * featuresPerFrame, featuresPerFrame)
            }
            return result
        }
    }
    
    /**
     * Convert buffer to flat FloatArray, padding with zeros if not full.
     * Useful for testing or when you want to run inference before buffer is full.
     * 
     * @return FloatArray of size (windowSize × featuresPerFrame), padded with zeros if needed
     */
    fun toFloatArrayPadded(): FloatArray {
        lock.withLock {
            val result = FloatArray(windowSize * featuresPerFrame)
            val currentSize = buffer.size
            
            // Copy existing frames
            for (i in 0 until currentSize) {
                System.arraycopy(buffer[i], 0, result, i * featuresPerFrame, featuresPerFrame)
            }
            
            // Remaining values are already 0.0f (default for FloatArray)
            
            return result
        }
    }
    
    /**
     * Clear all frames from buffer.
     */
    fun clear() {
        lock.withLock {
            buffer.clear()
            Log.d("KeypointsBuffer cleared")
        }
    }
    
    /**
     * Get a copy of the buffer as a list of frames.
     * Useful for debugging or visualization.
     * 
     * @return List of FloatArray frames (each of size featuresPerFrame)
     */
    fun getFrames(): List<FloatArray> {
        lock.withLock {
            return buffer.map { it.copyOf() }
        }
    }
    
    /**
     * Get the most recent frame.
     * 
     * @return Most recent frame or null if buffer is empty
     */
    fun getLatestFrame(): FloatArray? {
        lock.withLock {
            return buffer.lastOrNull()?.copyOf()
        }
    }
    
    /**
     * Get the oldest frame.
     * 
     * @return Oldest frame or null if buffer is empty
     */
    fun getOldestFrame(): FloatArray? {
        lock.withLock {
            return buffer.firstOrNull()?.copyOf()
        }
    }
    
    /**
     * Get buffer fill percentage.
     * 
     * @return Fill percentage [0, 100]
     */
    fun getFillPercentage(): Int {
        lock.withLock {
            return (buffer.size * 100) / windowSize
        }
    }
    
    override fun toString(): String {
        lock.withLock {
            return "KeypointsBuffer(size=${buffer.size}/$windowSize, fill=${getFillPercentage()}%)"
        }
    }
}

