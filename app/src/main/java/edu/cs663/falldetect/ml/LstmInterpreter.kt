package edu.cs663.falldetect.ml

import android.content.Context
import edu.cs663.falldetect.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite interpreter for LSTM fall detection model.
 * Loads lstm_fp16.tflite from assets and runs inference.
 */
class LstmInterpreter private constructor(
    private val interpreter: Interpreter
) {
    
    companion object {
        private const val MODEL_PATH = "ml/exports_tflite/lstm_fp16.tflite"
        
        /**
         * Create an LstmInterpreter instance.
         * Returns null if model file is not found.
         */
        fun create(context: Context): LstmInterpreter? {
            return try {
                val modelBuffer = loadModelFile(context)
                val options = Interpreter.Options().apply {
                    // TODO: Uncomment for NNAPI acceleration
                    // setUseNNAPI(true)
                    
                    // TODO: Uncomment for GPU delegate
                    // addDelegate(GpuDelegate())
                    
                    setNumThreads(4)
                }
                
                val interpreter = Interpreter(modelBuffer, options)
                Log.i("LSTM model loaded successfully from $MODEL_PATH")
                LstmInterpreter(interpreter)
            } catch (e: Exception) {
                Log.e("Failed to load LSTM model from $MODEL_PATH", e)
                null
            }
        }
        
        private fun loadModelFile(context: Context): MappedByteBuffer {
            val fileDescriptor = context.assets.openFd(MODEL_PATH)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }
    
    /**
     * Run inference on the buffered feature tensor.
     * 
     * @param bufferTensor Input tensor with shape [1][T][D]
     * @return Fall probability (0.0 to 1.0)
     */
    fun predict(bufferTensor: Array<Array<FloatArray>>): Float {
        // Output tensor: [1][1] for binary classification probability
        val output = Array(1) { FloatArray(1) }
        
        try {
            interpreter.run(bufferTensor, output)
            val probability = output[0][0]
            
            Log.d("LSTM prediction: $probability")
            return probability.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e("LSTM inference failed", e)
            // Return safe default on error
            return 0.01f
        }
    }
    
    /**
     * Release interpreter resources.
     */
    fun close() {
        interpreter.close()
        Log.i("LSTM interpreter closed")
    }
}

