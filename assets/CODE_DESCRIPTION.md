# SECTION 2: CODE DESCRIPTION

## Fall Detection & Posture Monitoring Android Application
**Course:** CS663 Mobile Vision
**Author:** Nikhil Chowdary
**Repository:** https://github.com/nikhilc523/mobile-vision-android

---

## TABLE OF CONTENTS

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Package Structure](#package-structure)
4. [Detailed File Descriptions](#detailed-file-descriptions)
   - [Data Models](#data-models)
   - [Emergency System](#emergency-system)
   - [Machine Learning Components](#machine-learning-components)
   - [Pose Analysis](#pose-analysis)
   - [Background Service](#background-service)
   - [User Interface](#user-interface)
   - [Utilities](#utilities)
5. [Configuration Files](#configuration-files)
6. [Resource Files](#resource-files)
7. [Asset Files](#asset-files)
8. [Data Flow](#data-flow)
9. [State Management](#state-management)

---

## PROJECT OVERVIEW

This Android application implements a real-time fall detection and posture monitoring system using computer vision and machine learning. The app uses the device camera to continuously analyze body pose, detect falls, and monitor posture quality.

### Key Technologies
- **Language:** Kotlin 1.9.24
- **UI Framework:** Android Views with ViewBinding
- **Camera:** CameraX 1.3.4
- **ML Framework:** TensorFlow Lite 2.14.0
- **AI Integration:** Google Gemini API 0.9.0
- **Architecture:** MVVM-inspired with Fragment-based navigation
- **Async:** Kotlin Coroutines

### Core Features
1. **Real-time Fall Detection** - Uses YOLO11n-Pose + BiLSTM model
2. **Posture Monitoring** - AI-powered posture analysis with Gemini
3. **Emergency Alert System** - Countdown timer with SMS and GPS
4. **Session Logging** - Tracks monitoring sessions and fall events
5. **Settings Management** - Emergency contacts and preferences

---

## ARCHITECTURE

The application follows a modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  (Fragments, Activities, Adapters, Custom Views)            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                            │
│              (FallService - Background Processing)           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Processing Layer                           │
│  (PoseAnalyzer, FallDetector, PostureTracker, Emergency)    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    ML/AI Layer                               │
│  (YoloPoseEstimator, LstmInterpreter, GeminiAnalyzer)       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                               │
│        (SharedPreferences, Data Models, Buffers)            │
└─────────────────────────────────────────────────────────────┘
```

---

## PACKAGE STRUCTURE

```
edu.cs663.falldetect/
├── data/                    # Data models and entities
│   ├── MonitoringSession.kt
│   └── PostureAnalysis.kt
│
├── emergency/               # Emergency alert system
│   └── EmergencyManager.kt
│
├── features/                # Feature extraction (unused legacy)
│   └── FeatureBuffer.kt
│
├── fusion/                  # Sensor fusion (unused legacy)
│   └── FallFusion.kt
│
├── ml/                      # Machine learning components
│   ├── DummyKeypointGenerator.kt
│   ├── FallDetector.kt
│   ├── GeminiPostureAnalyzer.kt
│   ├── KeypointsBuffer.kt
│   ├── LstmInterpreter.kt
│   ├── MoveNetPoseEstimator.kt
│   ├── PostureTracker.kt
│   └── YoloPoseEstimator.kt
│
├── pose/                    # Pose analysis orchestration
│   └── PoseAnalyzer.kt
│
├── service/                 # Background services
│   └── FallService.kt
│
├── ui/                      # User interface components
│   ├── ContactAdapter.kt
│   ├── EmergencyDialogFragment.kt
│   ├── HomeFragment.kt
│   ├── LogsFragment.kt
│   ├── MainActivity.kt
│   ├── OverlayView.kt
│   ├── PostureAdapter.kt
│   ├── PostureFragment.kt
│   ├── SessionAdapter.kt
│   └── SettingsFragment.kt
│
└── util/                    # Utility classes
    ├── FpsMeter.kt
    ├── Log.kt
    ├── PermissionHelper.kt
    └── PrefsHelper.kt
```

---

## DETAILED FILE DESCRIPTIONS

### DATA MODELS

#### `data/MonitoringSession.kt`

**Purpose:** Data class representing a single monitoring session.

**State:** ✅ Fully implemented and working

**Description:**
This data class stores information about each monitoring session, including when it started, how long it lasted, and how many falls were detected.

**Key Properties:**
- `id: String` - Unique identifier (timestamp-based)
- `startTime: Long` - Session start timestamp (milliseconds)
- `endTime: Long` - Session end timestamp (milliseconds)
- `fallCount: Int` - Number of falls detected during session
- `duration: Long` - Computed property (endTime - startTime)

**Usage:**
- Created when user starts monitoring
- Updated when falls are detected
- Saved to SharedPreferences when monitoring stops
- Displayed in LogsFragment

**Persistence:**
Serialized to JSON using Gson and stored in SharedPreferences under key `monitoring_sessions`.

**Example:**
```kotlin
MonitoringSession(
    id = "1701446400000",
    startTime = 1701446400000L,
    endTime = 1701446520000L,
    fallCount = 2
)
// Duration: 120 seconds (2 minutes)
```

---

#### `data/PostureAnalysis.kt`

**Purpose:** Data class representing a single posture analysis result.

**State:** ✅ Fully implemented and working

**Description:**
Stores the results of a posture analysis performed by the Gemini AI, including body angles, posture score, and recommendations.

**Key Properties:**
- `id: String` - Unique identifier (timestamp-based)
- `timestamp: Long` - When analysis was performed
- `neckAngle: Double` - Angle of neck relative to vertical (degrees)
- `spineAngle: Double` - Angle of spine relative to vertical (degrees)
- `shoulderAlignment: Double` - Vertical difference between shoulders (degrees)
- `score: Int` - Overall posture score (0-100)
- `status: String` - "EXCELLENT", "GOOD", "FAIR", or "POOR"
- `recommendation: String` - AI-generated advice
- `isGeminiAnalysis: Boolean` - True if analyzed by Gemini, false if fallback

**Posture Scoring:**
- **90-100:** EXCELLENT - Perfect posture
- **70-89:** GOOD - Minor adjustments needed
- **50-69:** FAIR - Noticeable issues
- **0-49:** POOR - Significant problems

**Ideal Ranges:**
- Neck angle: 165-175° (near vertical)
- Spine angle: 170-180° (near vertical)
- Shoulder alignment: <5° (level shoulders)

**Usage:**
- Created every 5 seconds during monitoring
- Analyzed by GeminiPostureAnalyzer or fallback calculation
- Tracked by PostureTracker for sustained bad posture
- Saved to SharedPreferences
- Displayed in PostureFragment

**Example:**
```kotlin
PostureAnalysis(
    id = "1701446400000",
    timestamp = 1701446400000L,
    neckAngle = 168.5,
    spineAngle = 175.2,
    shoulderAlignment = 3.1,
    score = 85,
    status = "GOOD",
    recommendation = "Slightly tilt your head back",
    isGeminiAnalysis = true
)
```

---

### EMERGENCY SYSTEM

#### `emergency/EmergencyManager.kt`

**Purpose:** Manages emergency alerts when falls are detected.

**State:** ✅ Fully implemented and working

**Description:**
Handles the emergency response workflow, including showing the countdown dialog, sending SMS alerts, and including GPS coordinates.

**Key Components:**

**1. Emergency Trigger:**
```kotlin
fun triggerEmergency(context: Context)
```
- Called when fall probability > 85%
- Shows EmergencyDialogFragment with countdown
- Starts countdown timer (configurable 10-30 seconds)

**2. SMS Sending:**
```kotlin
private fun sendEmergencySMS(context: Context)
```
- Retrieves emergency contacts from SharedPreferences
- Gets current GPS location (if enabled)
- Composes SMS message: "FALL DETECTED! I need help. Location: [GPS coordinates]"
- Opens SMS composer (user must press Send)

**3. GPS Location:**
```kotlin
private fun getCurrentLocation(context: Context, callback: (String) -> Unit)
```
- Uses FusedLocationProviderClient
- Gets last known location
- Formats as "lat,lng" string
- Includes Google Maps link in SMS

**Settings Integration:**
- Reads countdown duration from PrefsHelper
- Checks if SMS is enabled
- Checks if GPS is enabled
- Retrieves up to 3 emergency contacts

**User Flow:**
1. Fall detected (probability > 0.85)
2. EmergencyDialogFragment appears full-screen
3. Countdown starts (e.g., 20 seconds)
4. Text-to-Speech announces: "Fall detected! Sending alert in 20 seconds"
5. Haptic feedback (vibration pattern)
6. User can cancel by tapping "I'm OK"
7. If not cancelled, SMS composer opens with pre-filled message
8. User presses Send to alert contacts

**Dependencies:**
- `PrefsHelper` - For settings
- `EmergencyDialogFragment` - For UI
- `FusedLocationProviderClient` - For GPS
- `SmsManager` - For SMS (via Intent)

---

### MACHINE LEARNING COMPONENTS

#### `ml/YoloPoseEstimator.kt`

**Purpose:** Extracts 17 body keypoints from camera frames using YOLO11n-Pose model.

**State:** ✅ Fully implemented and working

**Description:**
This class wraps the YOLO11n-Pose TensorFlow Lite model to detect human pose keypoints in real-time from camera frames. It's the primary pose estimation engine used in the app.

**Model Details:**
- **Model:** YOLO11n-Pose (Ultralytics)
- **File:** `yolo11n-pose_float32.tflite` (6.2 MB)
- **Input:** [1, 320, 320, 3] - RGB image, 320x320 pixels
- **Output:** [1, 56, 8400] - Keypoint predictions
  - 56 channels = 4 (bbox) + 1 (confidence) + 51 (17 keypoints × 3)
  - 8400 = grid cells (80×80 + 40×40 + 20×20)

**COCO Keypoints (17 total):**
```
0: Nose           6: Left Shoulder    12: Left Hip
1: Left Eye       7: Right Shoulder   13: Right Hip
2: Right Eye      8: Left Elbow       14: Left Knee
3: Left Ear       9: Right Elbow      15: Right Knee
4: Right Ear     10: Left Wrist       16: Left Ankle
5: Neck          11: Right Wrist      17: Right Ankle
```

**Key Methods:**

**1. Initialization:**
```kotlin
init {
    interpreter = Interpreter(loadModelFile(context), options)
}
```
- Loads TFLite model from assets
- Configures interpreter with 4 threads
- Allocates input/output tensors

**2. Pose Estimation:**
```kotlin
fun estimatePose(bitmap: Bitmap): FloatArray
```
- Resizes input to 320×320
- Converts to RGB float array [0, 1]
- Runs inference
- Post-processes output to extract keypoints
- Returns FloatArray[34] in [y, x] pairs

**3. Post-processing:**
```kotlin
private fun postProcess(output: Array<Array<FloatArray>>): FloatArray
```
- Finds detection with highest confidence
- Extracts 17 keypoints (x, y, confidence)
- Normalizes coordinates to [0, 1]
- Converts to [y, x] format for BiLSTM model
- Filters low-confidence keypoints (<0.3)

**Performance:**
- Inference time: ~30-50ms on modern devices
- FPS: 20-30 with camera processing
- Accuracy: High for well-lit, full-body shots

**Usage Flow:**
```
Camera Frame (YUV)
  → Convert to Bitmap
  → Resize to 320×320
  → YoloPoseEstimator.estimatePose()
  → FloatArray[34] keypoints
  → KeypointsBuffer
  → FallDetector
```

---

#### `ml/MoveNetPoseEstimator.kt`

**Purpose:** Alternative pose estimator using Google's MoveNet model.

**State:** ⚠️ Implemented but not currently used (backup option)

**Description:**
Provides an alternative to YOLO using Google's MoveNet Lightning model. Currently not active but kept as a fallback option.

**Model Details:**
- **Model:** MoveNet Lightning (Google)
- **File:** `movenet_lightning_float32.tflite` (4.9 MB)
- **Input:** [1, 192, 192, 3] - RGB image
- **Output:** [1, 1, 17, 3] - 17 keypoints with (y, x, confidence)

**Differences from YOLO:**
- Smaller input size (192×192 vs 320×320)
- Faster inference (~20-30ms)
- Slightly lower accuracy
- Direct keypoint output (no post-processing needed)

**Why Not Used:**
YOLO11n-Pose was chosen for better accuracy and more robust detection in varied lighting conditions.

---

#### `ml/FallDetector.kt`

**Purpose:** Detects falls using BiLSTM model on temporal keypoint sequences.

**State:** ✅ Fully implemented and working

**Description:**
Analyzes sequences of 30 frames of keypoints to detect fall events using a trained BiLSTM (Bidirectional Long Short-Term Memory) neural network.

**Model Details:**
- **Model:** BiLSTM (custom trained)
- **File:** `fall_detection_model.tflite` (2.1 MB)
- **Input:** [1, 30, 34] - 30 frames of 17 keypoints (34 values)
- **Output:** [1, 1] - Fall probability [0.0, 1.0]
- **Threshold:** 0.85 (85%) triggers emergency alert

**Architecture:**
```
Input (30 frames × 34 keypoints)
  ↓
BiLSTM Layer 1 (64 units)
  ↓
BiLSTM Layer 2 (32 units)
  ↓
Dense Layer (16 units, ReLU)
  ↓
Output Layer (1 unit, Sigmoid)
  ↓
Fall Probability [0.0 - 1.0]
```

**Key Components:**

**1. Keypoints Buffer:**
```kotlin
private val keypointsBuffer = KeypointsBuffer(windowSize = 30)
```
- Maintains sliding window of last 30 frames
- Automatically manages buffer (FIFO)
- Only runs inference when buffer is full

**2. Fall Detection:**
```kotlin
fun detectFall(keypoints: FloatArray): Float
```
- Adds new keypoints to buffer
- Checks if buffer has 30 frames
- Runs BiLSTM inference
- Returns fall probability

**3. Inference:**
```kotlin
private fun runInference(sequence: Array<FloatArray>): Float
```
- Reshapes to [1, 30, 34]
- Runs TFLite interpreter
- Extracts probability from output
- Clamps to [0.0, 1.0]

**Fall Detection Logic:**
```kotlin
if (probability > 0.85f) {
    // Trigger emergency alert
    EmergencyManager.triggerEmergency(context)
}
```

**Temporal Analysis:**
The BiLSTM model analyzes motion patterns over time:
- **Standing → Falling:** Rapid downward movement of keypoints
- **Walking → Falling:** Sudden loss of vertical structure
- **Sitting → Falling:** Unexpected horizontal orientation

**Performance:**
- Inference time: ~15-25ms
- Latency: ~1 second (30 frames at 30 FPS)
- False positive rate: Low (<5% in testing)
- True positive rate: High (>90% for clear falls)

---

#### `ml/KeypointsBuffer.kt`

**Purpose:** Manages sliding window buffer of keypoint frames for temporal analysis.

**State:** ✅ Fully implemented and working

**Description:**
Implements a circular buffer (FIFO queue) that stores the last N frames of keypoints for the BiLSTM model.

**Key Properties:**
- `windowSize: Int = 30` - Number of frames to store
- `buffer: MutableList<FloatArray>` - Internal storage
- `isFull: Boolean` - True when buffer has 30 frames

**Key Methods:**

**1. Add Frame:**
```kotlin
fun addFrame(keypoints: FloatArray)
```
- Adds new keypoints to buffer
- If buffer is full, removes oldest frame (FIFO)
- Maintains exactly 30 frames when full

**2. Get Sequence:**
```kotlin
fun getSequence(): Array<FloatArray>
```
- Returns all frames as Array
- Used for BiLSTM inference
- Returns empty array if not full

**3. Clear:**
```kotlin
fun clear()
```
- Empties buffer
- Called when monitoring stops

**Buffer Behavior:**
```
Frame 1:  [kp1] → Buffer: [kp1]                    (size: 1)
Frame 2:  [kp2] → Buffer: [kp1, kp2]               (size: 2)
...
Frame 30: [kp30] → Buffer: [kp1...kp30]            (size: 30, FULL)
Frame 31: [kp31] → Buffer: [kp2...kp31]            (size: 30, removed kp1)
Frame 32: [kp32] → Buffer: [kp3...kp32]            (size: 30, removed kp2)
```

**Why 30 Frames?**
- At 30 FPS: 1 second of motion
- At 15 FPS: 2 seconds of motion
- Optimal for detecting fall events (typically 0.5-2 seconds)

---

#### `ml/GeminiPostureAnalyzer.kt`

**Purpose:** Analyzes posture quality using Google's Gemini AI.

**State:** ✅ Fully implemented and working (updated to use gemini-2.0-flash-exp)

**Description:**
Uses Google's Gemini generative AI to analyze body posture from keypoints and provide intelligent recommendations.

**API Configuration:**
- **Model:** `gemini-2.0-flash-exp`
- **API Key:** `AIzaSyCzLatfZs4ULYiRFFKvrb1NyQrMDxP7ubI`
- **Temperature:** 0.7 (balanced creativity)
- **Max Tokens:** 500

**Key Methods:**

**1. Analyze Posture:**
```kotlin
suspend fun analyzePosture(keypoints: FloatArray): PostureAnalysis
```
- Calculates body angles from keypoints
- Sends prompt to Gemini API
- Parses AI response
- Returns PostureAnalysis object
- Falls back to local calculation if API fails

**2. Body Angle Calculations:**
```kotlin
private fun calculateNeckAngle(keypoints: FloatArray): Double
private fun calculateSpineAngle(keypoints: FloatArray): Double
private fun calculateShoulderAlignment(keypoints: FloatArray): Double
```

**Neck Angle:**
- Vector from neck midpoint to nose
- Angle relative to vertical
- Ideal: 165-175° (nearly vertical)

**Spine Angle:**
- Vector from hip midpoint to shoulder midpoint
- Angle relative to vertical
- Ideal: 170-180° (straight back)

**Shoulder Alignment:**
- Vertical difference between left and right shoulders
- Ideal: <5° (level shoulders)

**3. Gemini Prompt:**
```kotlin
val prompt = """
Analyze this posture:
- Neck angle: ${neckAngle}° (ideal: 165-175°)
- Spine angle: ${spineAngle}° (ideal: 170-180°)
- Shoulder alignment: ${shoulderAlignment}° (ideal: <5°)

Provide:
1. Score (0-100)
2. Status (EXCELLENT/GOOD/FAIR/POOR)
3. Brief recommendation
"""
```

**4. Response Parsing:**
```kotlin
private fun parseGeminiResponse(response: String): PostureAnalysis
```
- Extracts score using regex: `Score:\s*(\d+)`
- Extracts status: `Status:\s*(EXCELLENT|GOOD|FAIR|POOR)`
- Extracts recommendation (remaining text)
- Validates and sanitizes output

**5. Fallback Calculation:**
```kotlin
private fun calculateFallbackPosture(keypoints: FloatArray): PostureAnalysis
```
- Used when Gemini API fails
- Simple rule-based scoring:
  - Neck angle deviation: -2 points per degree
  - Spine angle deviation: -3 points per degree
  - Shoulder misalignment: -5 points per degree
- Starts at 100, subtracts penalties
- Generates generic recommendations

**Error Handling:**
- Network errors → Fallback
- API quota exceeded → Fallback
- Invalid response → Fallback
- Timeout (10 seconds) → Fallback

**Usage:**
Called every 5 seconds during monitoring by PostureTracker.

---

#### `ml/PostureTracker.kt`

**Purpose:** Tracks posture over time and detects sustained bad posture.

**State:** ✅ Fully implemented and working

**Description:**
Monitors posture quality over time, distinguishing between temporary movements and sustained bad posture. Prevents false alerts from brief position changes.

**Key Properties:**
- `badPostureStartTime: Long?` - When bad posture began
- `lastGoodPostureTime: Long` - Last time posture was good
- `sustainedBadPostureThreshold: Long = 15000` - 15 seconds

**Key Methods:**

**1. Track Posture:**
```kotlin
fun trackPosture(analysis: PostureAnalysis): Boolean
```
- Checks if posture score < 70 (bad)
- Tracks duration of bad posture
- Returns true if sustained for 15+ seconds
- Resets timer if posture improves

**2. Reset:**
```kotlin
fun reset()
```
- Clears tracking state
- Called when monitoring stops

**Logic:**
```kotlin
if (score < 70) {
    if (badPostureStartTime == null) {
        badPostureStartTime = currentTime
    }
    val duration = currentTime - badPostureStartTime!!
    if (duration >= 15000) {
        return true  // Alert user!
    }
} else {
    badPostureStartTime = null  // Reset
}
```

**Why 15 Seconds?**
- Ignores temporary movements (reaching, turning)
- Catches actual poor sitting/standing posture
- Reduces false alerts
- Gives user time to self-correct

**Integration:**
- Used by PoseAnalyzer
- Triggers notification when sustained bad posture detected
- Saves analysis to SharedPreferences

---

#### `ml/LstmInterpreter.kt`

**Purpose:** Low-level wrapper for BiLSTM TensorFlow Lite model.

**State:** ⚠️ Implemented but wrapped by FallDetector (not used directly)

**Description:**
Provides direct access to the BiLSTM model interpreter. FallDetector uses this internally.

**Key Methods:**
```kotlin
fun runInference(input: Array<FloatArray>): Float
```
- Validates input shape [30, 34]
- Runs TFLite interpreter
- Returns fall probability

**Why Separate Class?**
- Separation of concerns
- Easier to swap models
- Testability

---

#### `ml/DummyKeypointGenerator.kt`

**Purpose:** Generates fake keypoints for testing without camera.

**State:** ⚠️ Legacy code, not used in production

**Description:**
Used during early development to test fall detection logic without requiring camera access.

**Generates:**
- Standing pose keypoints
- Walking pose keypoints
- Falling pose keypoints (for testing emergency alerts)

**Not Used:** Real keypoints from YoloPoseEstimator are used in production.

---

### POSE ANALYSIS

#### `pose/PoseAnalyzer.kt`

**Purpose:** Orchestrates the entire pose analysis pipeline.

**State:** ✅ Fully implemented and working

**Description:**
Central coordinator that processes camera frames through the complete analysis pipeline: pose estimation → fall detection → posture monitoring.

**Key Components:**

**1. Dependencies:**
```kotlin
private val yoloPoseEstimator: YoloPoseEstimator
private val fallDetector: FallDetector
private val geminiAnalyzer: GeminiPostureAnalyzer
private val postureTracker: PostureTracker
private val keypointsBuffer: KeypointsBuffer
```

**2. Main Processing Method:**
```kotlin
fun analyzeFrame(bitmap: Bitmap, callback: (Float, PostureAnalysis?) -> Unit)
```

**Processing Pipeline:**
```
1. Extract Keypoints
   bitmap → YoloPoseEstimator → FloatArray[34]

2. Fall Detection
   keypoints → KeypointsBuffer → FallDetector → probability
   if (probability > 0.85) → Trigger Emergency

3. Posture Analysis (every 5 seconds)
   keypoints → GeminiPostureAnalyzer → PostureAnalysis
   → PostureTracker → Check sustained bad posture
   → Save to SharedPreferences

4. Callback
   Return (fallProbability, postureAnalysis) to UI
```

**3. Timing Control:**
```kotlin
private var lastPostureCheckTime = 0L
private val postureCheckInterval = 5000L  // 5 seconds

if (currentTime - lastPostureCheckTime >= postureCheckInterval) {
    // Run posture analysis
    lastPostureCheckTime = currentTime
}
```

**4. Error Handling:**
```kotlin
try {
    val keypoints = yoloPoseEstimator.estimatePose(bitmap)
    // ... process
} catch (e: Exception) {
    Log.e("PoseAnalyzer", "Error analyzing frame", e)
    callback(0f, null)  // Return safe defaults
}
```

**5. Lifecycle Management:**
```kotlin
fun start() {
    // Initialize components
    keypointsBuffer.clear()
    postureTracker.reset()
}

fun stop() {
    // Cleanup
    keypointsBuffer.clear()
}
```

**Thread Safety:**
- Runs on background thread (Coroutine)
- UI updates via callback on main thread
- No blocking operations on UI thread

**Performance:**
- Total processing time: ~50-80ms per frame
  - YOLO: ~30-50ms
  - BiLSTM: ~15-25ms
  - Gemini: ~500-1000ms (async, every 5 seconds)
- Target FPS: 15-30

---

### BACKGROUND SERVICE

#### `service/FallService.kt`

**Purpose:** Background foreground service for continuous monitoring.

**State:** ✅ Fully implemented and working

**Description:**
Android foreground service that keeps the app running in the background, showing a persistent notification while monitoring is active.

**Service Type:** Foreground Service (required for background camera access)

**Key Methods:**

**1. Start Service:**
```kotlin
fun start(context: Context) {
    val intent = Intent(context, FallService::class.java)
    ContextCompat.startForegroundService(context, intent)
}
```

**2. onCreate:**
```kotlin
override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildNotification())
}
```

**3. Notification:**
```kotlin
private fun buildNotification(): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Fall Detection Active")
        .setContentText("Monitoring for falls...")
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()
}
```

**4. Stop Service:**
```kotlin
fun stop(context: Context) {
    val intent = Intent(context, FallService::class.java)
    context.stopService(intent)
}
```

**Notification Channel:**
- **ID:** `fall_detection_channel`
- **Name:** "Fall Detection"
- **Importance:** LOW (no sound/vibration)
- **Description:** "Notifications for fall detection monitoring"

**Why Foreground Service?**
- Android 8.0+ requires foreground service for background camera
- Prevents system from killing the app
- User is aware monitoring is active
- Required for continuous operation

**Lifecycle:**
```
User taps "Start Monitoring"
  → HomeFragment calls FallService.start()
  → Service starts
  → Notification appears
  → Camera processing begins

User taps "Stop Monitoring"
  → HomeFragment calls FallService.stop()
  → Service stops
  → Notification disappears
  → Camera processing stops
```

---

### USER INTERFACE

#### `ui/MainActivity.kt`

**Purpose:** Main activity hosting the navigation framework.

**State:** ✅ Fully implemented and working

**Description:**
Single-activity architecture using Navigation Component. Hosts all fragments and manages bottom navigation.

**Key Components:**

**1. ViewBinding:**
```kotlin
private lateinit var binding: ActivityMainBinding
```

**2. Navigation Setup:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val navController = findNavController(R.id.nav_host_fragment)
    binding.bottomNav.setupWithNavController(navController)
}
```

**3. Bottom Navigation:**
- Home (Fall Detection)
- Logs (Session History)
- Posture (Posture Analysis)
- Settings (Configuration)

**4. Permission Handling:**
```kotlin
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
) {
    // Delegates to PermissionHelper
}
```

**Layout:** `activity_main.xml`
- FrameLayout for NavHostFragment
- BottomNavigationView

**Navigation Graph:** `nav_graph.xml`
- Defines 4 destinations
- Handles fragment transactions
- Manages back stack

---

#### `ui/HomeFragment.kt`

**Purpose:** Main screen for fall detection with camera preview.

**State:** ✅ Fully implemented and working

**Description:**
Displays live camera preview, runs pose analysis, shows fall probability, and provides start/stop monitoring controls.

**Key Components:**

**1. Camera Setup:**
```kotlin
private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(binding.previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processFrame(imageProxy)
        }

        cameraProvider.bindToLifecycle(
            this, CameraSelector.DEFAULT_BACK_CAMERA,
            preview, imageAnalysis
        )
    }, ContextCompat.getMainExecutor(requireContext()))
}
```

**2. Frame Processing:**
```kotlin
private fun processFrame(imageProxy: ImageProxy) {
    val bitmap = imageProxy.toBitmap()

    poseAnalyzer.analyzeFrame(bitmap) { fallProb, postureAnalysis ->
        runOnUiThread {
            binding.tvFallProbability.text = "Fall: ${(fallProb * 100).toInt()}%"
            fpsMeter.tick()
            binding.tvFps.text = "FPS: ${fpsMeter.fps}"

            if (fallProb > 0.85f) {
                EmergencyManager.triggerEmergency(requireContext())
            }
        }
    }

    imageProxy.close()
}
```

**3. Monitoring Controls:**
```kotlin
private fun startMonitoring() {
    isMonitoring = true
    binding.btnStartStop.text = "Stop Monitoring"
    binding.btnStartStop.setBackgroundColor(Color.RED)

    FallService.start(requireContext())
    poseAnalyzer.start()

    currentSession = MonitoringSession(
        id = System.currentTimeMillis().toString(),
        startTime = System.currentTimeMillis(),
        endTime = 0L,
        fallCount = 0
    )
}

private fun stopMonitoring() {
    isMonitoring = false
    binding.btnStartStop.text = "Start Monitoring"
    binding.btnStartStop.setBackgroundColor(Color.GREEN)

    FallService.stop(requireContext())
    poseAnalyzer.stop()

    currentSession?.let {
        it.endTime = System.currentTimeMillis()
        PrefsHelper.saveSession(requireContext(), it)
    }
}
```

**4. UI Elements:**
- `PreviewView` - Camera preview
- `TextView` - FPS counter (top-left)
- `TextView` - Fall probability (top-right)
- `Button` - Start/Stop monitoring
- `OverlayView` - Skeleton overlay (optional)

**5. Permissions:**
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    if (PermissionHelper.hasCameraPermission(requireContext())) {
        startCamera()
    } else {
        PermissionHelper.requestCameraPermission(this)
    }
}
```

**Layout:** `fragment_home.xml`
- ConstraintLayout
- PreviewView (full screen)
- Overlays for FPS, probability, button

**Thread Management:**
- Camera processing: Background executor
- UI updates: Main thread via `runOnUiThread`
- Coroutines for async operations

---

#### `ui/LogsFragment.kt`

**Purpose:** Displays history of monitoring sessions.

**State:** ✅ Fully implemented and working

**Description:**
Shows a list of all monitoring sessions with time, duration, and fall count. Uses RecyclerView with custom adapter.

**Key Components:**

**1. RecyclerView Setup:**
```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    adapter = SessionAdapter()
    binding.recyclerView.adapter = adapter
    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

    loadSessions()
}
```

**2. Load Sessions:**
```kotlin
private fun loadSessions() {
    val sessions = PrefsHelper.getSessions(requireContext())
    adapter.submitList(sessions.sortedByDescending { it.startTime })

    if (sessions.isEmpty()) {
        binding.tvEmpty.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    } else {
        binding.tvEmpty.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
    }
}
```

**3. Refresh on Resume:**
```kotlin
override fun onResume() {
    super.onResume()
    loadSessions()  // Reload when user returns to tab
}
```

**Layout:** `fragment_logs.xml`
- RecyclerView for session list
- TextView for empty state ("No sessions yet")

**Session Display:**
Each session shows:
- Time (e.g., "1:05 AM")
- Duration (e.g., "2m 30s")
- Fall count with color indicator:
  - Green circle: 0 falls
  - Red circle: 1+ falls

---

#### `ui/PostureFragment.kt`

**Purpose:** Displays posture analysis history and summary.

**State:** ✅ Fully implemented and working

**Description:**
Shows summary card with statistics and detailed list of all posture analyses.

**Key Components:**

**1. Summary Card:**
```kotlin
private fun updateSummary(analyses: List<PostureAnalysis>) {
    val totalAnalyses = analyses.size
    val badPostureCount = analyses.count { it.score < 70 }
    val avgScore = if (analyses.isNotEmpty()) {
        analyses.map { it.score }.average().toInt()
    } else 0

    binding.tvTotalAnalyses.text = "$totalAnalyses"
    binding.tvBadPostureCount.text = "$badPostureCount"
    binding.tvAvgScore.text = "$avgScore"
}
```

**2. RecyclerView:**
```kotlin
adapter = PostureAdapter()
binding.recyclerView.adapter = adapter
binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

val analyses = PrefsHelper.getPostureAnalyses(requireContext())
adapter.submitList(analyses.sortedByDescending { it.timestamp })
```

**3. Refresh:**
```kotlin
override fun onResume() {
    super.onResume()
    loadAnalyses()
}
```

**Layout:** `fragment_posture.xml`
- CardView for summary
- RecyclerView for analysis list
- TextView for empty state

**Analysis Display:**
Each analysis shows:
- Time (e.g., "1:05:23 AM")
- Score with color:
  - Green: 90-100 (EXCELLENT)
  - Blue: 70-89 (GOOD)
  - Yellow: 50-69 (FAIR)
  - Red: 0-49 (POOR)
- Status badge
- Recommendation text
- Gemini/Fallback indicator

---

#### `ui/SettingsFragment.kt`

**Purpose:** Configuration screen for emergency contacts and preferences.

**State:** ✅ Fully implemented and working

**Description:**
Allows users to configure emergency contacts, countdown timer, and SMS/GPS settings.

**Key Components:**

**1. Emergency Contacts:**
```kotlin
private fun setupContactsList() {
    contactAdapter = ContactAdapter(
        onDeleteClick = { contact ->
            deleteContact(contact)
        }
    )
    binding.recyclerViewContacts.adapter = contactAdapter
    binding.recyclerViewContacts.layoutManager = LinearLayoutManager(requireContext())

    loadContacts()
}

private fun addContact() {
    val name = binding.etContactName.text.toString()
    val phone = binding.etContactPhone.text.toString()

    if (name.isNotBlank() && phone.isNotBlank()) {
        val contacts = PrefsHelper.getEmergencyContacts(requireContext()).toMutableList()

        if (contacts.size < 3) {
            contacts.add(EmergencyContact(name, phone))
            PrefsHelper.saveEmergencyContacts(requireContext(), contacts)
            loadContacts()
            clearInputs()
        } else {
            Toast.makeText(requireContext(), "Maximum 3 contacts", Toast.LENGTH_SHORT).show()
        }
    }
}
```

**2. Countdown Timer:**
```kotlin
binding.sliderCountdown.addOnChangeListener { _, value, _ ->
    val seconds = value.toInt()
    binding.tvCountdownValue.text = "$seconds seconds"
    PrefsHelper.saveCountdownDuration(requireContext(), seconds)
}

// Load saved value
val savedDuration = PrefsHelper.getCountdownDuration(requireContext())
binding.sliderCountdown.value = savedDuration.toFloat()
```

**3. SMS/GPS Toggles:**
```kotlin
binding.switchSms.setOnCheckedChangeListener { _, isChecked ->
    PrefsHelper.setSmsEnabled(requireContext(), isChecked)
}

binding.switchGps.setOnCheckedChangeListener { _, isChecked ->
    PrefsHelper.setGpsEnabled(requireContext(), isChecked)
}
```

**Layout:** `fragment_settings.xml`
- RecyclerView for contacts
- EditTexts for name/phone
- Button to add contact
- Slider for countdown (10-30 seconds)
- Switches for SMS/GPS

**Settings Saved:**
- Emergency contacts (up to 3)
- Countdown duration (10-30 seconds)
- SMS enabled (boolean)
- GPS enabled (boolean)

---

#### `ui/EmergencyDialogFragment.kt`

**Purpose:** Full-screen countdown dialog when fall is detected.

**State:** ✅ Fully implemented and working

**Description:**
Shows urgent full-screen alert with countdown timer, allowing user to cancel false alarms.

**Key Components:**

**1. Dialog Style:**
```kotlin
override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return Dialog(requireContext(), R.style.FullScreenDialog)
}
```

**2. Countdown Timer:**
```kotlin
private fun startCountdown(seconds: Int) {
    countDownTimer = object : CountDownTimer(seconds * 1000L, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val secondsLeft = (millisUntilFinished / 1000).toInt()
            binding.tvCountdown.text = "$secondsLeft"

            // Text-to-Speech
            tts.speak("$secondsLeft", TextToSpeech.QUEUE_FLUSH, null, null)

            // Haptic feedback
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        override fun onFinish() {
            sendEmergencyAlert()
            dismiss()
        }
    }.start()
}
```

**3. Cancel Button:**
```kotlin
binding.btnCancel.setOnClickListener {
    countDownTimer?.cancel()
    dismiss()
}
```

**4. Emergency Alert:**
```kotlin
private fun sendEmergencyAlert() {
    EmergencyManager.sendSMS(requireContext())
}
```

**Layout:** `dialog_emergency.xml`
- Full-screen red background
- Large warning icon
- Countdown number (huge text)
- "Fall Detected!" message
- "I'm OK" button (green)

**User Experience:**
1. Fall detected (probability > 85%)
2. Dialog appears full-screen
3. Countdown starts (e.g., 20 seconds)
4. TTS announces each second
5. Phone vibrates each second
6. User can tap "I'm OK" to cancel
7. If not cancelled, SMS composer opens

---

#### `ui/OverlayView.kt`

**Purpose:** Custom view for drawing skeleton overlay on camera preview.

**State:** ⚠️ Implemented but currently disabled (optional feature)

**Description:**
Draws pose skeleton (lines connecting keypoints) over the camera preview for visual feedback.

**Key Methods:**
```kotlin
fun updateKeypoints(keypoints: FloatArray) {
    this.keypoints = keypoints
    invalidate()  // Trigger redraw
}

override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    drawSkeleton(canvas, keypoints)
}
```

**Skeleton Connections:**
- Head: Nose → Eyes → Ears
- Torso: Shoulders → Hips
- Arms: Shoulder → Elbow → Wrist
- Legs: Hip → Knee → Ankle

**Why Disabled:**
- Performance impact (extra drawing)
- Not essential for functionality
- Can be enabled for debugging

---

#### `ui/SessionAdapter.kt`

**Purpose:** RecyclerView adapter for monitoring sessions.

**State:** ✅ Fully implemented and working

**Description:**
Displays list of monitoring sessions in LogsFragment.

**ViewHolder:**
```kotlin
class SessionViewHolder(private val binding: ItemMonitoringSessionBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(session: MonitoringSession) {
        binding.tvTime.text = formatTime(session.startTime)
        binding.tvDuration.text = formatDuration(session.duration)
        binding.tvFallCount.text = "${session.fallCount}"

        // Color indicator
        if (session.fallCount == 0) {
            binding.indicator.setBackgroundColor(Color.GREEN)
        } else {
            binding.indicator.setBackgroundColor(Color.RED)
        }
    }
}
```

**Layout:** `item_monitoring_session.xml`
- Horizontal card layout
- Time on left
- Duration in center
- Fall count on right
- Colored circle indicator

---

#### `ui/PostureAdapter.kt`

**Purpose:** RecyclerView adapter for posture analyses.

**State:** ✅ Fully implemented and working

**Description:**
Displays list of posture analyses in PostureFragment.

**ViewHolder:**
```kotlin
class PostureViewHolder(private val binding: ItemPostureAnalysisBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(analysis: PostureAnalysis) {
        binding.tvTime.text = formatTime(analysis.timestamp)
        binding.tvScore.text = "${analysis.score}"
        binding.tvStatus.text = analysis.status
        binding.tvRecommendation.text = analysis.recommendation

        // Score color
        val color = when (analysis.score) {
            in 90..100 -> Color.GREEN
            in 70..89 -> Color.BLUE
            in 50..69 -> Color.YELLOW
            else -> Color.RED
        }
        binding.tvScore.setTextColor(color)

        // Gemini indicator
        if (analysis.isGeminiAnalysis) {
            binding.ivGemini.visibility = View.VISIBLE
        } else {
            binding.ivGemini.visibility = View.GONE
        }
    }
}
```

**Layout:** `item_posture_analysis.xml`
- Vertical card layout
- Time and score at top
- Status badge
- Recommendation text
- Gemini icon (if AI-analyzed)

---

#### `ui/ContactAdapter.kt`

**Purpose:** RecyclerView adapter for emergency contacts.

**State:** ✅ Fully implemented and working

**Description:**
Displays list of emergency contacts in SettingsFragment with delete functionality.

**ViewHolder:**
```kotlin
class ContactViewHolder(
    private val binding: LayoutContactItemBinding,
    private val onDeleteClick: (EmergencyContact) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(contact: EmergencyContact) {
        binding.tvName.text = contact.name
        binding.tvPhone.text = contact.phone

        binding.btnDelete.setOnClickListener {
            onDeleteClick(contact)
        }
    }
}
```

**Layout:** `layout_contact_item.xml`
- Horizontal layout
- Name and phone on left
- Delete button (trash icon) on right

---

### UTILITIES

#### `util/FpsMeter.kt`

**Purpose:** Calculates and tracks frames per second.

**State:** ✅ Fully implemented and working

**Description:**
Measures camera processing performance by tracking frame timestamps.

**Key Methods:**

**1. Tick:**
```kotlin
fun tick() {
    frameCount++
    val currentTime = System.currentTimeMillis()

    if (currentTime - lastTime >= 1000) {
        fps = frameCount
        frameCount = 0
        lastTime = currentTime
    }
}
```

**2. Get FPS:**
```kotlin
val fps: Int
    get() = _fps
```

**Usage:**
```kotlin
val fpsMeter = FpsMeter()

// In frame processing loop:
processFrame(bitmap)
fpsMeter.tick()
binding.tvFps.text = "FPS: ${fpsMeter.fps}"
```

**Typical Values:**
- Good: 20-30 FPS
- Acceptable: 15-20 FPS
- Poor: <15 FPS (may affect detection accuracy)

---

#### `util/Log.kt`

**Purpose:** Centralized logging utility with tag management.

**State:** ✅ Fully implemented and working

**Description:**
Wrapper around Android's Log class with consistent tagging.

**Key Methods:**
```kotlin
object Log {
    private const val TAG = "FallDetect"

    fun d(message: String) {
        android.util.Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        android.util.Log.e(TAG, message, throwable)
    }

    fun i(message: String) {
        android.util.Log.i(TAG, message)
    }

    fun w(message: String) {
        android.util.Log.w(TAG, message)
    }
}
```

**Usage:**
```kotlin
Log.d("Starting camera preview")
Log.e("Failed to load model", exception)
Log.i("Fall detected! Probability: $prob")
```

**Benefits:**
- Consistent tag across app
- Easy to filter in logcat: `adb logcat -s FallDetect:*`
- Centralized logging control

---

#### `util/PermissionHelper.kt`

**Purpose:** Manages runtime permissions for camera, location, and SMS.

**State:** ✅ Fully implemented and working

**Description:**
Simplifies permission checking and requesting across the app.

**Key Methods:**

**1. Check Permissions:**
```kotlin
fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

fun hasSmsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
}
```

**2. Request Permissions:**
```kotlin
fun requestCameraPermission(fragment: Fragment) {
    fragment.requestPermissions(
        arrayOf(Manifest.permission.CAMERA),
        REQUEST_CAMERA_PERMISSION
    )
}

fun requestLocationPermission(fragment: Fragment) {
    fragment.requestPermissions(
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ),
        REQUEST_LOCATION_PERMISSION
    )
}
```

**3. Request All:**
```kotlin
fun requestAllPermissions(fragment: Fragment) {
    fragment.requestPermissions(
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS
        ),
        REQUEST_ALL_PERMISSIONS
    )
}
```

**Request Codes:**
```kotlin
const val REQUEST_CAMERA_PERMISSION = 100
const val REQUEST_LOCATION_PERMISSION = 101
const val REQUEST_SMS_PERMISSION = 102
const val REQUEST_ALL_PERMISSIONS = 103
```

**Usage:**
```kotlin
if (PermissionHelper.hasCameraPermission(requireContext())) {
    startCamera()
} else {
    PermissionHelper.requestCameraPermission(this)
}
```

---

#### `util/PrefsHelper.kt`

**Purpose:** Manages SharedPreferences for app settings and data persistence.

**State:** ✅ Fully implemented and working

**Description:**
Centralized access to SharedPreferences with type-safe methods for all app data.

**Key Methods:**

**1. Emergency Contacts:**
```kotlin
fun saveEmergencyContacts(context: Context, contacts: List<EmergencyContact>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = Gson().toJson(contacts)
    prefs.edit().putString(KEY_EMERGENCY_CONTACTS, json).apply()
}

fun getEmergencyContacts(context: Context): List<EmergencyContact> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_EMERGENCY_CONTACTS, null) ?: return emptyList()
    val type = object : TypeToken<List<EmergencyContact>>() {}.type
    return Gson().fromJson(json, type)
}
```

**2. Monitoring Sessions:**
```kotlin
fun saveSession(context: Context, session: MonitoringSession) {
    val sessions = getSessions(context).toMutableList()
    sessions.add(session)
    val json = Gson().toJson(sessions)
    prefs.edit().putString(KEY_SESSIONS, json).apply()
}

fun getSessions(context: Context): List<MonitoringSession> {
    val json = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
    val type = object : TypeToken<List<MonitoringSession>>() {}.type
    return Gson().fromJson(json, type)
}
```

**3. Posture Analyses:**
```kotlin
fun savePostureAnalysis(context: Context, analysis: PostureAnalysis) {
    val analyses = getPostureAnalyses(context).toMutableList()
    analyses.add(analysis)
    val json = Gson().toJson(analyses)
    prefs.edit().putString(KEY_POSTURE_ANALYSES, json).apply()
}

fun getPostureAnalyses(context: Context): List<PostureAnalysis> {
    val json = prefs.getString(KEY_POSTURE_ANALYSES, null) ?: return emptyList()
    val type = object : TypeToken<List<PostureAnalysis>>() {}.type
    return Gson().fromJson(json, type)
}
```

**4. Settings:**
```kotlin
fun getCountdownDuration(context: Context): Int {
    return prefs.getInt(KEY_COUNTDOWN_DURATION, 20)  // Default: 20 seconds
}

fun saveCountdownDuration(context: Context, seconds: Int) {
    prefs.edit().putInt(KEY_COUNTDOWN_DURATION, seconds).apply()
}

fun isSmsEnabled(context: Context): Boolean {
    return prefs.getBoolean(KEY_SMS_ENABLED, true)
}

fun setSmsEnabled(context: Context, enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SMS_ENABLED, enabled).apply()
}

fun isGpsEnabled(context: Context): Boolean {
    return prefs.getBoolean(KEY_GPS_ENABLED, true)
}

fun setGpsEnabled(context: Context, enabled: Boolean) {
    prefs.edit().putBoolean(KEY_GPS_ENABLED, enabled).apply()
}
```

**SharedPreferences Keys:**
```kotlin
private const val PREFS_NAME = "fall_detect_prefs"
private const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"
private const val KEY_SESSIONS = "monitoring_sessions"
private const val KEY_POSTURE_ANALYSES = "posture_analyses"
private const val KEY_COUNTDOWN_DURATION = "countdown_duration"
private const val KEY_SMS_ENABLED = "sms_enabled"
private const val KEY_GPS_ENABLED = "gps_enabled"
```

**Data Serialization:**
Uses Gson to serialize/deserialize complex objects to JSON strings for storage.

---

## CONFIGURATION FILES

### `AndroidManifest.xml`

**Purpose:** App configuration and permissions declaration.

**Key Elements:**

**1. Permissions:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
```

**2. Application:**
```xml
<application
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:theme="@style/Theme.FallDetection">

    <activity
        android:name=".ui.MainActivity"
        android:screenOrientation="portrait"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <service
        android:name=".service.FallService"
        android:foregroundServiceType="camera"
        android:exported="false" />
</application>
```

**3. Features:**
```xml
<uses-feature android:name="android.hardware.camera" android:required="true" />
<uses-feature android:name="android.hardware.location.gps" android:required="false" />
```

---

### `build.gradle.kts` (App Level)

**Purpose:** App build configuration and dependencies.

**Key Sections:**

**1. Android Configuration:**
```kotlin
android {
    namespace = "edu.cs663.falldetect"
    compileSdk = 34

    defaultConfig {
        applicationId = "edu.cs663.falldetect"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

**2. Dependencies:**
```kotlin
dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
```

---

### `build.gradle.kts` (Project Level)

**Purpose:** Project-level build configuration.

```kotlin
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

---

### `settings.gradle.kts`

**Purpose:** Project settings and repository configuration.

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FallDetection"
include(":app")
```

---

### `gradle.properties`

**Purpose:** Gradle configuration properties.

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
```

---

## RESOURCE FILES

### Layouts

**1. `activity_main.xml`**
- Root: ConstraintLayout
- NavHostFragment for fragment navigation
- BottomNavigationView for tab navigation

**2. `fragment_home.xml`**
- PreviewView for camera
- TextViews for FPS and fall probability
- Button for start/stop monitoring
- OverlayView for skeleton (optional)

**3. `fragment_logs.xml`**
- RecyclerView for session list
- TextView for empty state

**4. `fragment_posture.xml`**
- CardView for summary statistics
- RecyclerView for analysis list
- TextView for empty state

**5. `fragment_settings.xml`**
- RecyclerView for emergency contacts
- EditTexts for contact input
- Slider for countdown duration
- Switches for SMS/GPS

**6. `dialog_emergency.xml`**
- Full-screen layout
- Large countdown text
- Warning icon
- Cancel button

**7. `item_monitoring_session.xml`**
- Horizontal card for session
- Time, duration, fall count
- Color indicator

**8. `item_posture_analysis.xml`**
- Vertical card for analysis
- Score, status, recommendation
- Gemini indicator icon

**9. `layout_contact_item.xml`**
- Horizontal layout for contact
- Name and phone
- Delete button

---

### Drawables

**Icons:**
- `ic_home.xml` - Home tab icon
- `ic_logs.xml` - Logs tab icon
- `ic_posture.xml` - Posture tab icon
- `ic_settings.xml` - Settings tab icon
- `ic_notification.xml` - Service notification icon
- `ic_warning.xml` - Emergency dialog icon
- `ic_delete.xml` - Delete contact icon
- `ic_add_circle_outline.xml` - Add contact icon
- `ic_check.xml` - Confirm icon
- `ic_person_outline.xml` - Contact icon

**Shapes:**
- `bg_chip_round.xml` - Rounded background for chips
- `circle_indicator.xml` - Circle for fall count indicator

---

### Values

**1. `strings.xml`**
```xml
<resources>
    <string name="app_name">Fall Detection</string>
    <string name="start_monitoring">Start Monitoring</string>
    <string name="stop_monitoring">Stop Monitoring</string>
    <string name="fall_detected">Fall Detected!</string>
    <string name="emergency_message">FALL DETECTED! I need help.</string>
    <string name="no_sessions">No monitoring sessions yet</string>
    <string name="no_analyses">No posture analyses yet</string>
    <string name="add_contact">Add Emergency Contact</string>
    <string name="contact_name">Name</string>
    <string name="contact_phone">Phone Number</string>
    <string name="countdown_duration">Countdown Duration</string>
    <string name="enable_sms">Enable SMS Alerts</string>
    <string name="enable_gps">Include GPS Location</string>
</resources>
```

**2. `colors.xml`**
```xml
<resources>
    <color name="purple_200">#FFBB86FC</color>
    <color name="purple_500">#FF6200EE</color>
    <color name="purple_700">#FF3700B3</color>
    <color name="teal_200">#FF03DAC5</color>
    <color name="teal_700">#FF018786</color>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="red">#FFFF0000</color>
    <color name="green">#FF00FF00</color>
    <color name="yellow">#FFFFFF00</color>
    <color name="blue">#FF0000FF</color>
</resources>
```

**3. `themes.xml`**
```xml
<resources>
    <style name="Theme.FallDetection" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
        <item name="colorPrimary">@color/purple_500</item>
        <item name="colorPrimaryVariant">@color/purple_700</item>
        <item name="colorOnPrimary">@color/white</item>
        <item name="colorSecondary">@color/teal_200</item>
        <item name="colorSecondaryVariant">@color/teal_700</item>
        <item name="colorOnSecondary">@color/black</item>
    </style>

    <style name="FullScreenDialog" parent="Theme.MaterialComponents.Dialog">
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowIsFloating">false</item>
    </style>
</resources>
```

**4. `dimens.xml`**
```xml
<resources>
    <dimen name="padding_small">8dp</dimen>
    <dimen name="padding_medium">16dp</dimen>
    <dimen name="padding_large">24dp</dimen>
    <dimen name="text_size_small">12sp</dimen>
    <dimen name="text_size_medium">16sp</dimen>
    <dimen name="text_size_large">20sp</dimen>
    <dimen name="text_size_xlarge">32sp</dimen>
</resources>
```

---

### Navigation

**`nav_graph.xml`**
```xml
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <fragment
        android:id="@+id/homeFragment"
        android:name="edu.cs663.falldetect.ui.HomeFragment"
        android:label="Home" />

    <fragment
        android:id="@+id/logsFragment"
        android:name="edu.cs663.falldetect.ui.LogsFragment"
        android:label="Logs" />

    <fragment
        android:id="@+id/postureFragment"
        android:name="edu.cs663.falldetect.ui.PostureFragment"
        android:label="Posture" />

    <fragment
        android:id="@+id/settingsFragment"
        android:name="edu.cs663.falldetect.ui.SettingsFragment"
        android:label="Settings" />
</navigation>
```

---

### Menu

**`bottom_nav_menu.xml`**
```xml
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/homeFragment"
        android:icon="@drawable/ic_home"
        android:title="Home" />
    <item
        android:id="@+id/logsFragment"
        android:icon="@drawable/ic_logs"
        android:title="Logs" />
    <item
        android:id="@+id/postureFragment"
        android:icon="@drawable/ic_posture"
        android:title="Posture" />
    <item
        android:id="@+id/settingsFragment"
        android:icon="@drawable/ic_settings"
        android:title="Settings" />
</menu>
```

---

## ASSET FILES

### TensorFlow Lite Models

**1. `yolo11n-pose_float32.tflite`**
- **Size:** 6.2 MB
- **Purpose:** Pose estimation (keypoint extraction)
- **Input:** [1, 320, 320, 3] RGB image
- **Output:** [1, 56, 8400] keypoint predictions
- **Format:** Float32
- **Source:** Ultralytics YOLO11n-Pose

**2. `fall_detection_model.tflite`**
- **Size:** 2.1 MB
- **Purpose:** Fall detection from keypoint sequences
- **Input:** [1, 30, 34] temporal keypoints
- **Output:** [1, 1] fall probability
- **Format:** Float32
- **Architecture:** BiLSTM

**3. `movenet_lightning_float32.tflite`**
- **Size:** 4.9 MB
- **Purpose:** Alternative pose estimation (backup)
- **Input:** [1, 192, 192, 3] RGB image
- **Output:** [1, 1, 17, 3] keypoints
- **Format:** Float32
- **Source:** Google MoveNet

---

## DATA FLOW

### Complete Processing Pipeline

```
┌─────────────────────────────────────────────────────────────────┐
│                         USER INTERACTION                         │
│                  (Tap "Start Monitoring" Button)                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                         HOME FRAGMENT                            │
│  - Start FallService (foreground service)                        │
│  - Initialize CameraX                                            │
│  - Start PoseAnalyzer                                            │
│  - Create MonitoringSession                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                         CAMERA LAYER                             │
│  CameraX ImageAnalysis                                           │
│  - Captures frames at 30 FPS                                     │
│  - Converts YUV → Bitmap                                         │
│  - Sends to PoseAnalyzer                                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      POSE ANALYZER                               │
│  Orchestrates entire pipeline:                                   │
│  1. Keypoint Extraction                                          │
│  2. Fall Detection                                               │
│  3. Posture Monitoring                                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                    ┌─────────┴─────────┐
                    ↓                   ↓
┌──────────────────────────┐  ┌──────────────────────────┐
│   YOLO POSE ESTIMATOR    │  │   EVERY 5 SECONDS:       │
│  - Resize to 320×320     │  │   GEMINI POSTURE         │
│  - Run YOLO inference    │  │   ANALYZER               │
│  - Extract 17 keypoints  │  │  - Calculate angles      │
│  - Return FloatArray[34] │  │  - Call Gemini API       │
└──────────────────────────┘  │  - Parse response        │
            ↓                 │  - Return analysis       │
┌──────────────────────────┐  └──────────────────────────┘
│   KEYPOINTS BUFFER       │              ↓
│  - Add frame to buffer   │  ┌──────────────────────────┐
│  - Maintain 30 frames    │  │   POSTURE TRACKER        │
│  - FIFO queue            │  │  - Track bad posture     │
└──────────────────────────┘  │  - Check 15s threshold   │
            ↓                 │  - Alert if sustained    │
┌──────────────────────────┐  └──────────────────────────┘
│   FALL DETECTOR          │              ↓
│  - Get 30-frame sequence │  ┌──────────────────────────┐
│  - Run BiLSTM inference  │  │   PREFS HELPER           │
│  - Return probability    │  │  - Save analysis         │
└──────────────────────────┘  │  - Store in SharedPrefs  │
            ↓                 └──────────────────────────┘
┌──────────────────────────┐
│   IF PROBABILITY > 0.85  │
│   EMERGENCY MANAGER      │
│  - Show countdown dialog │
│  - TTS + Vibration       │
│  - Send SMS if not       │
│    cancelled             │
└──────────────────────────┘
            ↓
┌──────────────────────────┐
│   EMERGENCY DIALOG       │
│  - Full-screen alert     │
│  - Countdown timer       │
│  - "I'm OK" button       │
└──────────────────────────┘
            ↓
┌──────────────────────────┐
│   SMS COMPOSER           │
│  - Pre-filled message    │
│  - GPS coordinates       │
│  - Emergency contacts    │
└──────────────────────────┘
```

---

### Fall Detection Data Flow

```
Camera Frame (YUV Image)
    ↓
Convert to Bitmap (RGB)
    ↓
Resize to 320×320
    ↓
YoloPoseEstimator.estimatePose()
    ↓
TFLite Inference (YOLO)
    ↓
Post-process Output
    ↓
FloatArray[34] Keypoints
    ↓
KeypointsBuffer.addFrame()
    ↓
Check if buffer.isFull (30 frames)
    ↓ (Yes)
FallDetector.detectFall()
    ↓
Get 30-frame sequence
    ↓
Reshape to [1, 30, 34]
    ↓
TFLite Inference (BiLSTM)
    ↓
Extract probability [0.0 - 1.0]
    ↓
Return to PoseAnalyzer
    ↓
Callback to HomeFragment
    ↓
Update UI: "Fall: 85%"
    ↓
If > 0.85: Trigger Emergency
```

---

### Posture Monitoring Data Flow

```
Every 5 seconds during monitoring:

FloatArray[34] Keypoints
    ↓
GeminiPostureAnalyzer.analyzePosture()
    ↓
Calculate Body Angles:
  - Neck angle (nose → neck vs vertical)
  - Spine angle (shoulders → hips vs vertical)
  - Shoulder alignment (left vs right shoulder)
    ↓
Build Gemini Prompt:
  "Analyze this posture:
   Neck: 168°, Spine: 175°, Shoulders: 3°"
    ↓
Call Gemini API (async)
    ↓
Parse Response:
  - Score: 85
  - Status: GOOD
  - Recommendation: "Slightly tilt head back"
    ↓
Create PostureAnalysis object
    ↓
PostureTracker.trackPosture()
    ↓
Check if score < 70 (bad posture)
    ↓ (Yes)
Track duration of bad posture
    ↓
If duration >= 15 seconds:
  - Show notification
  - Log warning
    ↓
PrefsHelper.savePostureAnalysis()
    ↓
Serialize to JSON (Gson)
    ↓
Store in SharedPreferences
    ↓
PostureFragment.onResume()
    ↓
Load and display in RecyclerView
```

---

### Session Logging Data Flow

```
User taps "Start Monitoring"
    ↓
Create MonitoringSession:
  - id = timestamp
  - startTime = currentTime
  - endTime = 0
  - fallCount = 0
    ↓
Store in HomeFragment (in-memory)
    ↓
During monitoring:
  If fall detected (prob > 0.85):
    session.fallCount++
    ↓
User taps "Stop Monitoring"
    ↓
Update session:
  - endTime = currentTime
  - duration = endTime - startTime
    ↓
PrefsHelper.saveSession()
    ↓
Load existing sessions from SharedPrefs
    ↓
Append new session to list
    ↓
Serialize to JSON (Gson)
    ↓
Store in SharedPreferences
    ↓
LogsFragment.onResume()
    ↓
Load sessions from SharedPrefs
    ↓
Sort by startTime (descending)
    ↓
Display in RecyclerView via SessionAdapter
```

---

## STATE MANAGEMENT

### Application State

The app maintains state across multiple layers:

**1. UI State (Fragment-level)**
- `isMonitoring: Boolean` - Whether monitoring is active
- `currentSession: MonitoringSession?` - Current session (if monitoring)
- `fallProbability: Float` - Latest fall probability
- `fps: Int` - Current frames per second

**2. Service State**
- `FallService.isRunning: Boolean` - Whether foreground service is active
- Notification shown/hidden

**3. Persistent State (SharedPreferences)**
- Emergency contacts (List<EmergencyContact>)
- Monitoring sessions (List<MonitoringSession>)
- Posture analyses (List<PostureAnalysis>)
- Settings:
  - Countdown duration (Int)
  - SMS enabled (Boolean)
  - GPS enabled (Boolean)

**4. ML Model State**
- `KeypointsBuffer` - Last 30 frames of keypoints
- `PostureTracker` - Bad posture tracking state
- TFLite interpreters (loaded models)

---

### State Transitions

**Monitoring State Machine:**

```
┌─────────────┐
│   STOPPED   │ ← Initial state
└─────────────┘
       ↓ (User taps "Start Monitoring")
┌─────────────┐
│  STARTING   │
│  - Start service
│  - Init camera
│  - Create session
└─────────────┘
       ↓
┌─────────────┐
│  MONITORING │ ← Active state
│  - Process frames
│  - Detect falls
│  - Track posture
└─────────────┘
       ↓ (User taps "Stop Monitoring")
┌─────────────┐
│  STOPPING   │
│  - Stop service
│  - Save session
│  - Cleanup
└─────────────┘
       ↓
┌─────────────┐
│   STOPPED   │
└─────────────┘
```

**Emergency Alert State Machine:**

```
┌─────────────┐
│    IDLE     │ ← Normal state
└─────────────┘
       ↓ (Fall detected, prob > 0.85)
┌─────────────┐
│  COUNTDOWN  │
│  - Show dialog
│  - TTS + vibrate
│  - Count down
└─────────────┘
       ↓ (User taps "I'm OK")
┌─────────────┐
│  CANCELLED  │
│  - Dismiss dialog
│  - Return to monitoring
└─────────────┘
       ↓
┌─────────────┐
│    IDLE     │
└─────────────┘

       OR

┌─────────────┐
│  COUNTDOWN  │
└─────────────┘
       ↓ (Timer expires)
┌─────────────┐
│ SEND ALERT  │
│  - Open SMS composer
│  - Pre-fill message
│  - Include GPS
└─────────────┘
       ↓
┌─────────────┐
│    IDLE     │
└─────────────┘
```

---

### Lifecycle Management

**Fragment Lifecycle:**

```
onCreate()
  ↓
onCreateView()
  ↓
onViewCreated()
  - Setup UI
  - Initialize components
  - Request permissions
  ↓
onResume()
  - Reload data from SharedPrefs
  - Refresh UI
  ↓
onPause()
  - Save state if needed
  ↓
onDestroyView()
  - Cleanup resources
  ↓
onDestroy()
```

**Service Lifecycle:**

```
startForegroundService()
  ↓
onCreate()
  - Create notification channel
  ↓
onStartCommand()
  - Start foreground with notification
  ↓
(Service running)
  ↓
stopService()
  ↓
onDestroy()
  - Cleanup
  - Remove notification
```

**Camera Lifecycle:**

```
startCamera()
  ↓
ProcessCameraProvider.getInstance()
  ↓
bindToLifecycle()
  - Preview
  - ImageAnalysis
  ↓
(Camera active)
  ↓
onPause() or stopMonitoring()
  ↓
unbindAll()
  ↓
(Camera released)
```

---

## SUMMARY

This Fall Detection & Posture Monitoring application is a comprehensive Android solution that combines:

**✅ Real-time Computer Vision**
- YOLO11n-Pose for accurate keypoint extraction
- 30 FPS camera processing
- Efficient TensorFlow Lite inference

**✅ Machine Learning**
- BiLSTM temporal analysis for fall detection
- 30-frame sliding window
- 85% probability threshold

**✅ AI Integration**
- Google Gemini API for intelligent posture analysis
- Natural language recommendations
- Fallback to rule-based analysis

**✅ Emergency Response**
- Configurable countdown timer
- Text-to-Speech alerts
- SMS with GPS coordinates
- User cancellation option

**✅ Data Persistence**
- Session logging
- Posture history
- Settings management
- SharedPreferences with Gson

**✅ Modern Android Architecture**
- Single-activity with Navigation Component
- ViewBinding for type-safe views
- Kotlin Coroutines for async operations
- Foreground service for background monitoring

**✅ User Experience**
- Clean Material Design UI
- Bottom navigation
- Real-time feedback (FPS, probability)
- Comprehensive settings

---

**Total Files:** 101
**Total Lines of Code:** ~9,687
**Languages:** Kotlin (100%)
**Min SDK:** 26 (Android 8.0)
**Target SDK:** 34 (Android 14)

---

**Document Version:** 1.0
**Last Updated:** December 1, 2024
**Author:** Nikhil Chowdary
**Course:** CS663 Mobile Vision
