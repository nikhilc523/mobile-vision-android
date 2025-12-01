package edu.cs663.falldetect.features

import edu.cs663.falldetect.util.Log

/**
 * Ring buffer for storing feature vectors over time.
 * Maintains a sliding window of T timesteps, each with D features.
 * 
 * @param timesteps Number of timesteps to buffer (T=60 by default)
 * @param featureDim Dimension of each feature vector (D)
 */
class FeatureBuffer(
    private val timesteps: Int = 60,
    private val featureDim: Int
) {
    private val buffer = Array(timesteps) { FloatArray(featureDim) }
    private var currentIndex = 0
    private var isFull = false
    
    /**
     * Add a new feature vector to the buffer.
     * Overwrites the oldest entry when buffer is full.
     */
    fun add(features: FloatArray) {
        require(features.size == featureDim) {
            "Feature vector size ${features.size} doesn't match expected dimension $featureDim"
        }
        
        buffer[currentIndex] = features.copyOf()
        currentIndex = (currentIndex + 1) % timesteps
        
        if (currentIndex == 0) {
            isFull = true
        }
    }
    
    /**
     * Check if buffer has enough data for inference.
     */
    fun isReady(): Boolean = isFull
    
    /**
     * Convert buffer to tensor format for TFLite inference.
     * Returns shape: [1][T][D] where T=timesteps, D=featureDim
     */
    fun asTensor(): Array<Array<FloatArray>> {
        if (!isFull) {
            Log.w("FeatureBuffer not full yet, returning partial data")
        }
        
        // Create tensor with shape [1][T][D]
        val tensor = Array(1) { Array(timesteps) { FloatArray(featureDim) } }
        
        // Copy buffer in chronological order
        for (i in 0 until timesteps) {
            val bufferIdx = (currentIndex + i) % timesteps
            tensor[0][i] = buffer[bufferIdx].copyOf()
        }
        
        return tensor
    }
    
    /**
     * Clear the buffer.
     */
    fun clear() {
        currentIndex = 0
        isFull = false
        buffer.forEach { it.fill(0f) }
    }
    
    /**
     * Get current buffer size.
     */
    fun size(): Int = if (isFull) timesteps else currentIndex
}

