TensorFlow Lite Model Placement
================================

Place your trained LSTM fall detection model here:

Required file: lstm_fp16.tflite

The model should:
- Accept input shape: [1][60][D] where D is the feature dimension
- Output shape: [1][1] representing fall probability (0.0 to 1.0)
- Be quantized to FP16 for optimal performance

If the model file is missing, the app will:
- Log a warning message
- Continue running with degraded functionality
- Return a safe default probability (0.01)

To add your model:
1. Train your LSTM model
2. Convert to TensorFlow Lite format with FP16 quantization
3. Copy the .tflite file to this directory
4. Rebuild the app

The LstmInterpreter class will automatically load the model from this path.

