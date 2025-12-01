package edu.cs663.falldetect.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import edu.cs663.falldetect.util.Log

/**
 * MoveNet SinglePose Lightning estimator for real-time pose detection.
 * Extracts 17 COCO keypoints from camera frames.
 * 
 * This implementation matches the training data format:
 * - Uses MoveNet (same as training, NOT YOLO)
 * - Outputs [x, y] coordinates (NOT [y, x])
 * - COCO keypoint order (17 keypoints)
 * - Normalized to [0, 1] range
 * - Confidence threshold: 0.3
 */
class MoveNetPoseEstimator(context: Context) {
    
    companion object {
        private const val TAG = "MoveNetPoseEstimator"
        private const val MODEL_NAME = "movenet_lightning_float32.tflite"
        private const val INPUT_SIZE = 192  // MoveNet Lightning uses 192×192
        private const val NUM_KEYPOINTS = 17  // COCO format
        private const val CONFIDENCE_THRESHOLD = 0.1f  // Lowered from 0.3 to accept more keypoints
        
        // COCO keypoint names (for debugging)
        private val KEYPOINT_NAMES = arrayOf(
            "nose", "left_eye", "right_eye", "left_ear", "right_ear",
            "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
            "left_wrist", "right_wrist", "left_hip", "right_hip",
            "left_knee", "right_knee", "left_ankle", "right_ankle"
        )
    }
    
    private val interpreter: Interpreter

    init {
        Log.d("Initializing MoveNet SinglePose Lightning (192×192, CPU optimized)...", tag = TAG)

        // Load model
        val modelFile = loadModelFile(context, MODEL_NAME)

        // Create interpreter options
        val options = Interpreter.Options()

        // Use CPU with 4 threads (MoveNet is optimized for CPU)
        options.setNumThreads(4)
        Log.d("Using CPU (4 threads)", tag = TAG)

        interpreter = Interpreter(modelFile, options)

        Log.d("✅ MoveNet model loaded successfully", tag = TAG)
        logModelInfo()
    }
    
    /**
     * Extract keypoints from camera frame.
     * 
     * @param bitmap Camera frame (any size)
     * @return FloatArray of size 34 (17 keypoints × 2 coordinates [x, y])
     */
    fun extractKeypoints(bitmap: Bitmap): FloatArray {
        try {
            // Preprocess image
            val inputBuffer = preprocessImage(bitmap)
            
            // Prepare output buffer
            val outputBuffer = prepareOutputBuffer()
            
            // Run inference
            val startTime = System.currentTimeMillis()
            interpreter.run(inputBuffer, outputBuffer)
            val inferenceTime = System.currentTimeMillis() - startTime
            
            // Parse output
            val keypoints = parseOutput(outputBuffer)
            
            Log.d("Inference time: ${inferenceTime}ms", tag = TAG)
            
            return keypoints
            
        } catch (e: Exception) {
            Log.e("Error extracting keypoints: ${e.message}", e, tag = TAG)
            // Return zeros on error
            return FloatArray(34)
        }
    }
    
    /**
     * Preprocess image for MoveNet input.
     * 
     * Input: Bitmap (any size)
     * Output: ByteBuffer (1, 192, 192, 3) with int32 values in [0, 255]
     * 
     * IMPORTANT: MoveNet expects int32 input in [0, 255] range (NOT normalized to [0, 1])
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize to 192×192
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        // Create ByteBuffer for int32 input
        // Size: 1 (batch) × 192 (height) × 192 (width) × 3 (RGB) × 4 (bytes per int32)
        val buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        buffer.order(ByteOrder.nativeOrder())
        
        // Extract pixels
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        
        // Convert to int32 RGB values [0, 255]
        for (pixel in pixels) {
            // Extract RGB channels (keep as int32 in [0, 255] range)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            // Store as int32 (NOT float!)
            buffer.putInt(r)
            buffer.putInt(g)
            buffer.putInt(b)
        }
        
        return buffer
    }
    
    /**
     * Prepare output buffer for MoveNet.
     * 
     * MoveNet output: (1, 1, 17, 3)
     * - 1 = batch size
     * - 1 = number of persons (single pose)
     * - 17 = number of keypoints (COCO format)
     * - 3 = [y, x, confidence] for each keypoint
     */
    private fun prepareOutputBuffer(): Array<Array<Array<FloatArray>>> {
        // Create output array: [1][1][17][3]
        return Array(1) {
            Array(1) {
                Array(NUM_KEYPOINTS) {
                    FloatArray(3)
                }
            }
        }
    }
    
    /**
     * Parse MoveNet output to extract keypoints.
     * 
     * MoveNet output format:
     * - Shape: (1, 1, 17, 3)
     * - Format: [y, x, confidence] for each of 17 keypoints
     * - Coordinates are already normalized to [0, 1]
     * 
     * BiLSTM expected format:
     * - FloatArray(34) - 17 keypoints × 2 coordinates
     * - Format: [x, y] for each keypoint (SWAP from MoveNet's [y, x]!)
     * - COCO order (same as MoveNet)
     * - Normalized to [0, 1] (already done by MoveNet)
     * 
     * @return FloatArray(34) - 17 keypoints × 2 (x, y) normalized to [0, 1]
     */
    private fun parseOutput(outputBuffer: Array<Array<Array<FloatArray>>>): FloatArray {
        val keypoints = FloatArray(34)
        
        // Extract keypoints from output: [1][1][17][3]
        val keypointsRaw = outputBuffer[0][0]  // Shape: [17][3]
        
        // Find average confidence to determine if person is detected
        var totalConfidence = 0f
        var validKeypointsCount = 0
        
        for (kptIdx in 0 until NUM_KEYPOINTS) {
            val y = keypointsRaw[kptIdx][0]      // MoveNet outputs y first
            val x = keypointsRaw[kptIdx][1]      // Then x
            val conf = keypointsRaw[kptIdx][2]   // Then confidence
            
            totalConfidence += conf
            if (conf > CONFIDENCE_THRESHOLD) {
                validKeypointsCount++
            }
        }
        
        val avgConfidence = totalConfidence / NUM_KEYPOINTS
        
        // Log person detection
        if (validKeypointsCount < 5) {
            Log.d("⚠️ Low confidence person detection (avg conf: ${"%.2f".format(avgConfidence)}, valid: $validKeypointsCount/17)", tag = TAG)
        } else {
            Log.d("✅ Person detected (avg conf: ${"%.2f".format(avgConfidence)}, valid keypoints: $validKeypointsCount/17)", tag = TAG)
        }
        
        // Debug: Log raw MoveNet output for nose
        val noseY = keypointsRaw[0][0]
        val noseX = keypointsRaw[0][1]
        val noseConf = keypointsRaw[0][2]
        Log.d("Raw MoveNet nose: y=$noseY, x=$noseX, conf=$noseConf", tag = TAG)

        // Debug: Log all keypoints with their confidence (we use all of them now!)
        val allKpts = mutableListOf<String>()
        for (kptIdx in 0 until NUM_KEYPOINTS) {
            val conf = keypointsRaw[kptIdx][2]
            allKpts.add("${KEYPOINT_NAMES[kptIdx]}(${"%.2f".format(conf)})")
        }
        Log.d("All keypoints (using all regardless of confidence): ${allKpts.take(5).joinToString(", ")}...", tag = TAG)
        
        // Extract keypoints in BiLSTM format
        for (kptIdx in 0 until NUM_KEYPOINTS) {
            val y = keypointsRaw[kptIdx][0]
            val x = keypointsRaw[kptIdx][1]
            val conf = keypointsRaw[kptIdx][2]

            // CRITICAL FIX: Always use keypoints, regardless of confidence!
            // The BiLSTM model was trained on data with NO ZEROS.
            // Setting low-confidence keypoints to 0.0 creates out-of-distribution data
            // that the model has never seen, causing near-zero probabilities.
            // Even low-confidence keypoints are better than zeros!

            // CRITICAL: Store as [y, x] to match training format!
            // MoveNet outputs [y, x, conf] and BiLSTM expects [y, x]
            // Training code comment: "34 features = 17 keypoints × 2 coordinates (y, x)"
            keypoints[kptIdx * 2] = y.coerceIn(0f, 1f)      // ✅ y first!
            keypoints[kptIdx * 2 + 1] = x.coerceIn(0f, 1f)  // ✅ x second!
        }
        
        // Debug: Log first 3 keypoints (nose, left_eye, right_eye) in [y, x] format
        Log.d("Keypoints sample (y,x): nose=[${keypoints[0]}, ${keypoints[1]}], left_eye=[${keypoints[2]}, ${keypoints[3]}], right_eye=[${keypoints[4]}, ${keypoints[5]}]", tag = TAG)
        
        // Debug: Log all keypoints to see the full picture
        val kptStr = KEYPOINT_NAMES.mapIndexed { i, name ->
            "$name=[${String.format("%.3f", keypoints[i*2])}, ${String.format("%.3f", keypoints[i*2+1])}]"
        }.joinToString(", ")
        Log.d("All keypoints: $kptStr", tag = TAG)
        
        return keypoints
    }
    
    /**
     * Load TFLite model from assets.
     */
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Log model input/output info.
     */
    private fun logModelInfo() {
        val inputShape = interpreter.getInputTensor(0).shape()
        val outputShape = interpreter.getOutputTensor(0).shape()
        
        Log.d("Input shape: ${inputShape.contentToString()}", tag = TAG)
        Log.d("Output shape: ${outputShape.contentToString()}", tag = TAG)
        Log.d("Expected input: [1, 192, 192, 3] int32", tag = TAG)
        Log.d("Expected output: [1, 1, 17, 3] float32", tag = TAG)
    }
    
    /**
     * Clean up resources.
     */
    fun close() {
        interpreter.close()
        Log.d("MoveNetPoseEstimator closed", tag = TAG)
    }
}

