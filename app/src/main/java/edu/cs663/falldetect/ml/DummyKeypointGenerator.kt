package edu.cs663.falldetect.ml

import java.util.Random

/**
 * Dummy keypoint generator for testing fall detection without YOLO pose estimation.
 * 
 * Generates realistic keypoint patterns for:
 * - Normal standing/walking activity
 * - Fall sequences (standing → falling → on ground)
 * 
 * Keypoint Format:
 * - 34 values per frame (17 COCO keypoints × 2 coordinates [y, x])
 * - All values normalized to [0, 1]
 * - y = vertical position (0 = top, 1 = bottom)
 * - x = horizontal position (0 = left, 1 = right)
 * 
 * COCO Keypoint Order:
 * 0: nose, 1: left_eye, 2: right_eye, 3: left_ear, 4: right_ear,
 * 5: left_shoulder, 6: right_shoulder, 7: left_elbow, 8: right_elbow,
 * 9: left_wrist, 10: right_wrist, 11: left_hip, 12: right_hip,
 * 13: left_knee, 14: right_knee, 15: left_ankle, 16: right_ankle
 */
object DummyKeypointGenerator {

    // Use Java Random for Gaussian distribution
    private val random = Random()

    /**
     * Generate dummy keypoints for one frame (34 values).
     * Simulates a person standing upright.
     *
     * @return FloatArray of size 34 (17 keypoints × 2 coords [y, x])
     */
    fun generateNormalFrame(): FloatArray {
        val keypoints = FloatArray(34)

        // IMPORTANT: Match training data format from test_tflite_model.py
        // Training uses: np.random.randn() * 0.1 + 0.5, clipped to [0, 1]
        // This creates Gaussian distribution with mean=0.5, std=0.1
        for (i in 0 until 34) {
            // Generate Gaussian random value (mean=0, std=1)
            val gaussian = random.nextGaussian().toFloat()
            // Scale to match training data: mean=0.5, std=0.1
            val value = gaussian * 0.1f + 0.5f
            // Clip to [0, 1] range
            keypoints[i] = value.coerceIn(0f, 1f)
        }

        return keypoints
    }
    
    /**
     * Generate dummy keypoints for a fall sequence (30 frames).
     * Simulates: standing → falling → on ground
     * 
     * Expected behavior:
     * - Frames 0-10: Normal standing (probability ~10-30%)
     * - Frames 10-20: Falling (probability increases)
     * - Frames 20-30: On ground (probability ~99%, FALL DETECTED)
     * 
     * @return List of 30 FloatArrays, each of size 34
     */
    fun generateFallSequence(): List<FloatArray> {
        val sequence = mutableListOf<FloatArray>()
        
        // Frames 0-10: Normal standing
        for (t in 0 until 10) {
            sequence.add(generateNormalFrame())
        }
        
        // Frames 10-20: Falling (y-coordinates increasing = moving down)
        for (t in 10 until 20) {
            val keypoints = FloatArray(34)
            val fallProgress = (t - 10) / 10f  // 0.0 to 1.0

            for (i in 0 until 17) {
                // y-coordinate: move from 0.3 to 0.8 (falling down)
                keypoints[i * 2] = 0.3f + fallProgress * 0.5f + random.nextFloat() * 0.05f
                // x-coordinate: slight horizontal movement
                keypoints[i * 2 + 1] = 0.5f + random.nextFloat() * 0.1f - 0.05f
            }

            sequence.add(keypoints)
        }

        // Frames 20-30: On ground (stillness)
        // All keypoints at same y-level (person lying flat)
        val groundY = 0.75f + random.nextFloat() * 0.05f

        for (t in 20 until 30) {
            val keypoints = FloatArray(34)

            for (i in 0 until 17) {
                // All keypoints at similar y (on ground)
                keypoints[i * 2] = groundY + random.nextFloat() * 0.05f
                // x-coordinates spread out (body lying horizontally)
                keypoints[i * 2 + 1] = 0.3f + (i / 17f) * 0.4f + random.nextFloat() * 0.05f
            }

            sequence.add(keypoints)
        }
        
        return sequence
    }
    
    /**
     * Generate dummy keypoints for normal activity sequence (30 frames).
     * Simulates: walking, standing, slight movements
     * 
     * Expected behavior:
     * - All frames: Probability ~10-30%
     * - No fall detection alert
     * 
     * @return List of 30 FloatArrays, each of size 34
     */
    fun generateNormalSequence(): List<FloatArray> {
        val sequence = mutableListOf<FloatArray>()
        
        for (t in 0 until 30) {
            sequence.add(generateNormalFrame())
        }
        
        return sequence
    }
    
    /**
     * Generate a single frame with all keypoints on the ground (for testing).
     * This should trigger fall detection immediately when buffer is full.
     * 
     * @return FloatArray of size 34 with all keypoints at ground level
     */
    fun generateGroundFrame(): FloatArray {
        val keypoints = FloatArray(34)
        val groundY = 0.75f + random.nextFloat() * 0.05f

        for (i in 0 until 17) {
            keypoints[i * 2] = groundY + random.nextFloat() * 0.05f  // y (on ground)
            keypoints[i * 2 + 1] = 0.3f + (i / 17f) * 0.4f + random.nextFloat() * 0.05f  // x (spread)
        }

        return keypoints
    }
    
    /**
     * Validate that keypoints array has correct format.
     * 
     * @param keypoints The keypoints array to validate
     * @return true if valid, false otherwise
     */
    fun validateKeypoints(keypoints: FloatArray): Boolean {
        // Check size
        if (keypoints.size != 34) {
            return false
        }
        
        // Check all values are in [0, 1] range
        for (value in keypoints) {
            if (value < 0f || value > 1f) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Print keypoints in human-readable format (for debugging).
     * 
     * @param keypoints The keypoints array to print
     * @param label Optional label for the output
     */
    fun printKeypoints(keypoints: FloatArray, label: String = "Keypoints") {
        println("$label (${keypoints.size} values):")
        
        val keypointNames = arrayOf(
            "nose", "left_eye", "right_eye", "left_ear", "right_ear",
            "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle"
        )
        
        for (i in 0 until 17) {
            val y = keypoints[i * 2]
            val x = keypoints[i * 2 + 1]
            println("  ${keypointNames[i]}: y=${"%.3f".format(y)}, x=${"%.3f".format(x)}")
        }
    }
}

