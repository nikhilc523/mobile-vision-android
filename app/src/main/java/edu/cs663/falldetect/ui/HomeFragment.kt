package edu.cs663.falldetect.ui

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import com.google.android.material.snackbar.Snackbar
import edu.cs663.falldetect.R
import edu.cs663.falldetect.databinding.FragmentHomeBinding
import edu.cs663.falldetect.data.PostureAnalysis
import edu.cs663.falldetect.ml.DummyKeypointGenerator
import edu.cs663.falldetect.ml.FallDetector
import edu.cs663.falldetect.ml.GeminiPostureAnalyzer
import edu.cs663.falldetect.ml.KeypointsBuffer
import edu.cs663.falldetect.ml.MoveNetPoseEstimator
import edu.cs663.falldetect.ml.PostureTracker
import edu.cs663.falldetect.service.FallService
import edu.cs663.falldetect.util.FpsMeter
import edu.cs663.falldetect.util.Log
import edu.cs663.falldetect.util.PermissionHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Home fragment with camera preview and monitoring controls.
 */
class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
        private const val KEY_MONITORING = "monitoring"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var isMonitoring = false
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val fpsMeter = FpsMeter()
    private lateinit var cameraExecutor: ExecutorService

    // Fall detection
    private var fallDetector: FallDetector? = null
    private val keypointsBuffer = KeypointsBuffer(windowSize = 30, featuresPerFrame = 34)
    private var moveNetPose: MoveNetPoseEstimator? = null

    // Posture analysis
    private var postureAnalyzer: GeminiPostureAnalyzer? = null
    private var postureAnalysisJob: Job? = null
    private val postureAnalysisInterval = 5000L // 5 seconds
    private val gson = Gson()
    private val postureTracker = PostureTracker(
        badPostureThreshold = 70,      // Score < 70 is "bad"
        sustainedDurationSeconds = 15  // Must be bad for 15+ seconds to save (reduced for testing)
    )

    // Session tracking
    private var sessionStartTime: Long = 0
    private var sessionFallCount: Int = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.i("All permissions granted")
            hidePermissionPlaceholder()
            // Start camera preview immediately after permissions granted
            bindCamera()
        } else {
            handlePermissionDenied()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize fall detector
        try {
            fallDetector = FallDetector(requireContext())
            Log.i("FallDetector initialized successfully")
        } catch (e: Exception) {
            Log.e("Failed to initialize FallDetector", e)
            // Continue without fall detection
        }

        // Initialize MoveNet pose estimator
        try {
            moveNetPose = MoveNetPoseEstimator(requireContext())
            Log.i("MoveNetPoseEstimator initialized successfully")
        } catch (e: Exception) {
            Log.e("Failed to initialize MoveNetPoseEstimator", e)
            // Continue without MoveNet (will use dummy keypoints)
        }

        // Initialize Gemini posture analyzer
        try {
            postureAnalyzer = GeminiPostureAnalyzer(requireContext())
            Log.i("GeminiPostureAnalyzer initialized successfully")
        } catch (e: Exception) {
            Log.e("Failed to initialize GeminiPostureAnalyzer", e)
            // Continue without posture analysis
        }

        // Restore state
        savedInstanceState?.let {
            isMonitoring = it.getBoolean(KEY_MONITORING, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        checkInitialPermissions()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_MONITORING, isMonitoring)
    }

    override fun onResume() {
        super.onResume()
        // Always bind camera when permissions are granted (for preview)
        if (PermissionHelper.hasAllPermissions(requireContext())) {
            bindCamera()
        }
    }

    override fun onPause() {
        super.onPause()
        unbindCamera()
    }

    private fun setupUI() {
        binding.btnStartMonitoring.setOnClickListener {
            checkPermissionsAndStart()
        }

        binding.btnStopMonitoring.setOnClickListener {
            stopMonitoring()
        }

        binding.btnGrantPermission.setOnClickListener {
            requestPermissions()
        }

        // Start FPS updates
        fpsMeter.startUpdates { fps ->
            binding.overlayView.setFps(fps)
        }

        // Show fall status card if fall detector is initialized
        if (fallDetector != null) {
            binding.cardFallStatus.visibility = View.VISIBLE
            updateBufferStatus()
        }
    }

    private fun checkInitialPermissions() {
        if (!PermissionHelper.hasAllPermissions(requireContext())) {
            showPermissionPlaceholder()
        } else {
            hidePermissionPlaceholder()
            // Start camera preview immediately when permissions are granted
            bindCamera()
        }
    }
    
    private fun checkPermissionsAndStart() {
        if (PermissionHelper.hasAllPermissions(requireContext())) {
            startMonitoring()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showPermissionRationale()
            } else {
                requestPermissions()
            }
        }
    }
    
    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permissions Required")
            .setMessage(getString(R.string.permission_camera_rationale) + "\n\n" +
                    getString(R.string.permission_location_rationale))
            .setPositiveButton("Grant") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun requestPermissions() {
        permissionLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS)
    }
    
    private fun handlePermissionDenied() {
        Snackbar.make(
            binding.root,
            R.string.permission_denied,
            Snackbar.LENGTH_LONG
        ).setAction(R.string.permission_settings) {
            openAppSettings()
        }.show()
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", requireContext().packageName, null)
        }
        startActivity(intent)
    }
    
    private fun startMonitoring() {
        Log.i("Starting fall detection monitoring")
        isMonitoring = true

        // Start session tracking
        sessionStartTime = System.currentTimeMillis()
        sessionFallCount = 0

        // Update UI
        binding.btnStartMonitoring.isEnabled = false
        binding.btnStopMonitoring.isEnabled = true

        // Show the fall detection status card
        binding.cardFallStatus.visibility = View.VISIBLE

        // Reset FPS meter
        fpsMeter.reset()

        // Start foreground service
        FallService.startService(requireContext())

        // Start posture analysis
        startPostureAnalysis()

        // Bind camera if permissions granted
        if (PermissionHelper.hasAllPermissions(requireContext())) {
            bindCamera()
        }
    }

    private fun stopMonitoring() {
        Log.i("Stopping fall detection monitoring")
        isMonitoring = false

        // Save session
        saveSession()

        // Update UI
        binding.btnStartMonitoring.isEnabled = true
        binding.btnStopMonitoring.isEnabled = false

        // Hide the fall detection status card
        binding.cardFallStatus.visibility = View.GONE

        // Clear keypoints buffer
        keypointsBuffer.clear()
        updateBufferStatus()

        // Stop posture analysis
        stopPostureAnalysis()

        // Unbind camera
        unbindCamera()

        // Stop foreground service
        FallService.stopService(requireContext())
    }

    private fun saveSession() {
        if (sessionStartTime == 0L) return

        val prefs = requireContext().getSharedPreferences("monitoring_sessions", android.content.Context.MODE_PRIVATE)
        val sessionCount = prefs.getInt("session_count", 0)

        // Save new session
        prefs.edit().apply {
            putLong("session_${sessionCount}_start", sessionStartTime)
            putLong("session_${sessionCount}_end", System.currentTimeMillis())
            putInt("session_${sessionCount}_falls", sessionFallCount)
            putInt("session_count", sessionCount + 1)
            apply()
        }

        Log.i("Session saved: $sessionFallCount falls, duration: ${(System.currentTimeMillis() - sessionStartTime) / 1000}s")
    }
    
    private fun bindCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e("Failed to bind camera", e)
                Snackbar.make(
                    binding.root,
                    R.string.error_camera_init,
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        // Unbind all use cases before rebinding
        provider.unbindAll()

        // Get display rotation
        val rotation = binding.previewView.display?.rotation ?: Surface.ROTATION_0

        // Build Preview use case
        preview = Preview.Builder()
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

        // Build ImageAnalysis use case
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    // Tick FPS meter
                    fpsMeter.tick()

                    // Process frame for fall detection if monitoring
                    if (isMonitoring) {
                        processFrameForFallDetection(imageProxy)
                    }

                    // Close image
                    imageProxy.close()
                }
            }

        // Select back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Bind use cases to lifecycle
            provider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

            Log.i("Camera bound successfully")
        } catch (e: Exception) {
            Log.e("Failed to bind camera use cases", e)
        }
    }

    private fun unbindCamera() {
        cameraProvider?.unbindAll()
        preview = null
        imageAnalysis = null
    }

    private fun showPermissionPlaceholder() {
        binding.permissionPlaceholder.visibility = View.VISIBLE
    }

    private fun hidePermissionPlaceholder() {
        binding.permissionPlaceholder.visibility = View.GONE
    }

    private fun updateBufferStatus() {
        val bufferSize = keypointsBuffer.size()
        val fillPercentage = keypointsBuffer.getFillPercentage()
        binding.textBufferStatus.text = "Buffer: $bufferSize/30 frames ($fillPercentage%)"
    }

    private fun updateFallDetectionUI(probability: Float, isFall: Boolean) {
        binding.textFallProbability.text = "Probability: ${(probability * 100).toInt()}%"
        binding.textFallStatus.text = "Status: ${if (isFall) "FALL DETECTED" else "NO FALL"}"

        // Change text color based on result
        val color = if (isFall) {
            requireContext().getColor(android.R.color.holo_red_dark)
        } else {
            requireContext().getColor(android.R.color.holo_green_dark)
        }
        binding.textFallStatus.setTextColor(color)
    }

    /**
     * Process each camera frame for fall detection.
     * Extracts keypoints using YOLO11-Pose and adds to buffer.
     */
    private fun processFrameForFallDetection(imageProxy: ImageProxy) {
        // Extract keypoints from camera frame
        val keypoints = extractKeypoints(imageProxy)

        // 🔍 DEBUG: Log keypoint extraction
        val validCount = keypoints.count { it > 0.0f }
        Log.d("Keypoints extracted: $validCount/34 valid", tag = TAG)

        // Add to buffer
        keypointsBuffer.add(keypoints)

        // Update buffer status on UI thread
        activity?.runOnUiThread {
            updateBufferStatus()
        }

        // 🔍 DEBUG: Log buffer status
        Log.d("Buffer: ${keypointsBuffer.size()}/30, isFull=${keypointsBuffer.isFull()}", tag = TAG)

        // Run inference when buffer is full
        if (keypointsBuffer.isFull()) {
            Log.d("Buffer is full, running fall detection...", tag = TAG)
            runFallDetection()
        }
    }

    /**
     * Extract keypoints from camera frame using MoveNet.
     * Falls back to dummy keypoints if MoveNet is not available.
     */
    private fun extractKeypoints(imageProxy: ImageProxy): FloatArray {
        // Try to use MoveNet if available
        moveNetPose?.let { movenet ->
            try {
                // Convert ImageProxy to Bitmap
                val bitmap = imageProxyToBitmap(imageProxy)

                // Extract keypoints using MoveNet
                return movenet.extractKeypoints(bitmap)

            } catch (e: Exception) {
                Log.e("Error extracting keypoints with MoveNet: ${e.message}", e, tag = TAG)
                // Fall through to dummy keypoints
            }
        }

        // Fallback: Use dummy keypoints
        return DummyKeypointGenerator.generateNormalFrame()
    }

    /**
     * Convert ImageProxy to Bitmap for YOLO processing.
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /**
     * Run fall detection inference on the buffered keypoints.
     */
    private fun runFallDetection() {
        Log.d("runFallDetection() called, fallDetector=${fallDetector != null}", tag = TAG)

        fallDetector?.let { detector ->
            try {
                val input = keypointsBuffer.toFloatArray()

                // 🔍 DEBUG: Log input statistics
                val min = input.minOrNull() ?: 0f
                val max = input.maxOrNull() ?: 0f
                val avg = input.average().toFloat()
                Log.d("Input stats: min=${"%.3f".format(min)}, max=${"%.3f".format(max)}, avg=${"%.3f".format(avg)}, size=${input.size}", tag = TAG)

                val result = detector.detectFallWithResult(input)

                // 🔍 DEBUG: Log result
                Log.d("Fall detection result: probability=${result.probability}, isFall=${result.isFall}", tag = TAG)

                // Update UI on main thread
                activity?.runOnUiThread {
                    updateFallDetectionUI(result.probability, result.isFall)

                    // Trigger emergency if fall detected
                    if (result.isFall) {
                        Log.w("FALL DETECTED! Probability: ${result.getProbabilityPercent()}")
                        triggerEmergencyCountdown()
                    }
                }
            } catch (e: Exception) {
                Log.e("Failed to run fall detection", e)
            }
        } ?: run {
            Log.e("FallDetector is null! Cannot run inference", tag = TAG)
        }
    }

    /**
     * Trigger emergency countdown dialog when fall is detected.
     * This is the same dialog that was triggered by the button in LogsFragment.
     */
    private fun triggerEmergencyCountdown() {
        Log.i("Fall detected - triggering emergency countdown")

        // Increment fall count for this session
        sessionFallCount++

        // Launch full-screen emergency countdown dialog
        val dialog = EmergencyDialogFragment()
        dialog.show(childFragmentManager, "EmergencyDialog")
    }

    // ========== TEST METHODS (FOR DEVELOPMENT) ==========

    /**
     * Test fall detection with a simulated fall sequence.
     * This method can be called from developer menu or debug builds.
     *
     * Expected result:
     * - Frames 0-10: Probability ~10-30% (normal)
     * - Frames 10-20: Probability increases (falling)
     * - Frames 20-30: Probability ~99% (FALL DETECTED alert)
     */
    fun testFallSequence() {
        Log.i("Testing fall sequence...")

        // Stop monitoring if active
        if (isMonitoring) {
            stopMonitoring()
        }

        // Clear buffer
        keypointsBuffer.clear()
        updateBufferStatus()

        // Generate fall sequence (30 frames)
        val sequence = DummyKeypointGenerator.generateFallSequence()

        // Process each frame with delay to simulate 30 FPS
        cameraExecutor.execute {
            sequence.forEachIndexed { index, keypoints ->
                processTestFrame(keypoints, index)
                Thread.sleep(33)  // 33ms = ~30 FPS
            }

            Log.i("Fall sequence test completed")
        }
    }

    /**
     * Test fall detection with a normal activity sequence.
     * This method can be called from developer menu or debug builds.
     *
     * Expected result:
     * - All frames: Probability ~10-30%
     * - No fall detection alert
     */
    fun testNormalSequence() {
        Log.i("Testing normal sequence...")

        // Stop monitoring if active
        if (isMonitoring) {
            stopMonitoring()
        }

        // Clear buffer
        keypointsBuffer.clear()
        updateBufferStatus()

        // Generate normal sequence (30 frames)
        val sequence = DummyKeypointGenerator.generateNormalSequence()

        // Process each frame with delay to simulate 30 FPS
        cameraExecutor.execute {
            sequence.forEachIndexed { index, keypoints ->
                processTestFrame(keypoints, index)
                Thread.sleep(33)  // 33ms = ~30 FPS
            }

            Log.i("Normal sequence test completed")
        }
    }

    /**
     * Process a single test frame (used by test sequences).
     * Similar to processFrameForFallDetection but with frame index logging.
     */
    private fun processTestFrame(keypoints: FloatArray, frameIndex: Int) {
        // Validate keypoints
        if (!DummyKeypointGenerator.validateKeypoints(keypoints)) {
            Log.e("Invalid keypoints at frame $frameIndex")
            return
        }

        // Add to buffer
        keypointsBuffer.add(keypoints)

        // Update buffer status on UI thread
        activity?.runOnUiThread {
            updateBufferStatus()
        }

        // Run inference when buffer is full
        if (keypointsBuffer.isFull()) {
            fallDetector?.let { detector ->
                try {
                    val input = keypointsBuffer.toFloatArray()
                    val result = detector.detectFallWithResult(input)

                    Log.d("Frame $frameIndex: Probability = ${result.getProbabilityPercent()}, isFall = ${result.isFall}")

                    // Update UI on main thread
                    activity?.runOnUiThread {
                        updateFallDetectionUI(result.probability, result.isFall)

                        // Trigger emergency if fall detected
                        if (result.isFall) {
                            Log.w("FALL DETECTED at frame $frameIndex! Probability: ${result.getProbabilityPercent()}")
                            triggerEmergencyCountdown()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Failed to run fall detection at frame $frameIndex", e)
                }
            }
        }
    }

    // ========== POSTURE ANALYSIS METHODS ==========

    /**
     * Start periodic posture analysis using Gemini API.
     */
    private fun startPostureAnalysis() {
        postureAnalysisJob?.cancel()
        postureAnalysisJob = CoroutineScope(Dispatchers.Main).launch {
            while (isMonitoring) {
                delay(postureAnalysisInterval)
                analyzePosture()
            }
        }
        Log.i("Posture analysis started (every ${postureAnalysisInterval / 1000}s)")
    }

    /**
     * Stop posture analysis.
     */
    private fun stopPostureAnalysis() {
        postureAnalysisJob?.cancel()
        postureAnalysisJob = null
        postureTracker.reset()
        Log.i("Posture analysis stopped")
    }

    /**
     * Analyze current posture using the latest keypoints.
     */
    private fun analyzePosture() {
        postureAnalyzer?.let { analyzer ->
            // Get the latest keypoints from buffer
            val latestKeypoints = keypointsBuffer.getLatestFrame()
            if (latestKeypoints == null) {
                Log.d("No keypoints available for posture analysis")
                return
            }

            // Run analysis in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val analysis = analyzer.analyzePosture(latestKeypoints)
                    if (analysis != null) {
                        // Update posture tracker
                        val shouldSave = postureTracker.updatePosture(analysis)

                        if (shouldSave) {
                            // Only save if sustained bad posture detected
                            Log.w("⚠️ SUSTAINED BAD POSTURE! Score=${analysis.score}, Status=${analysis.status}, Duration=${postureTracker.getCurrentDuration()}s")
                            savePostureAnalysis(analysis)

                            // Show warning on UI
                            activity?.runOnUiThread {
                                showPostureWarning(analysis)
                            }
                        } else {
                            Log.d("Posture check: Score=${analysis.score}, Status=${analysis.status}, Duration=${postureTracker.getCurrentDuration()}s (not saving yet)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Failed to analyze posture: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Show posture warning on UI.
     */
    private fun showPostureWarning(analysis: PostureAnalysis) {
        val duration = postureTracker.getCurrentDuration()
        val message = "⚠️ Bad posture for ${duration}s! Score: ${analysis.score}/100"

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("View") {
                // Navigate to Posture tab
                // Note: This would require navigation component reference
            }
            .show()

        Log.w("Posture warning shown: $message")
    }

    /**
     * Save posture analysis to SharedPreferences.
     */
    private fun savePostureAnalysis(analysis: PostureAnalysis) {
        try {
            val prefs = requireContext().getSharedPreferences("posture_analyses", android.content.Context.MODE_PRIVATE)
            val analysesJson = prefs.getString("analyses_list", null)

            val analyses = mutableListOf<PostureAnalysis>()
            if (analysesJson != null) {
                val type = object : TypeToken<List<PostureAnalysis>>() {}.type
                val loadedAnalyses: List<PostureAnalysis> = gson.fromJson(analysesJson, type)
                analyses.addAll(loadedAnalyses)
            }

            // Add new analysis
            analyses.add(analysis)

            // Keep only last 100 analyses
            if (analyses.size > 100) {
                analyses.removeAt(0)
            }

            // Save back to SharedPreferences
            val updatedJson = gson.toJson(analyses)
            prefs.edit().putString("analyses_list", updatedJson).apply()

            Log.d("Posture analysis saved (total: ${analyses.size})")
        } catch (e: Exception) {
            Log.e("Failed to save posture analysis: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fpsMeter.stopUpdates()
        stopPostureAnalysis()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        fallDetector?.close()
        moveNetPose?.close()
    }
}
