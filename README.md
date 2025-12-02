# Fall Detection & Posture Monitoring App With Emergency Contact Alerts
---

## 📋 Project Overview

A production-ready Android application that provides:
1. **Real-time Fall Detection** using pose estimation and BiLSTM deep learning model
2. **Posture Monitoring** using Google Gemini AI for intelligent posture analysis
3. **Emergency Alert System** with countdown timer and SMS notifications
4. **Session Logging** to track monitoring history and fall events

### Key Features
- **Fall Detection**: YOLO11n-Pose + BiLSTM model analyzing 30-frame sliding window
- **Posture Analysis**: Gemini AI analyzing body angles every 5 seconds during monitoring
- **Emergency System**: Configurable countdown (10-30s) with TTS, haptic feedback, and SMS
- **Smart Tracking**: Only saves posture issues sustained for 15+ seconds
- **Session History**: Detailed logs of all monitoring sessions with fall counts

---

## 📖 SECTION 1: EXECUTION INSTRUCTIONS

### Prerequisites

Before you begin, ensure you have:
- **Android Studio**: Hedgehog (2023.1.1) or later
- **Android SDK**: Min SDK 26, Target SDK 34
- **Physical Android Device** (recommended) or Emulator with camera support
- **Git**: For cloning the repository

### Step 1.1: Download the Code from GitHub

1. Go to the repository: https://github.com/nikhilc523/mobile-vision-android
2. Click the green **"Code"** button
3. Click **"Download ZIP"**
4. Save the ZIP file to your Downloads folder

**Screenshot 1.1:** *(Upload to Canvas showing the GitHub repository page)*

### Step 1.2: Extract to a Temporary Directory

1. Create a folder called `temp` on your Desktop or Documents
2. Extract the downloaded ZIP file to this `temp` folder
3. You should see the following directory structure:

```
temp/
├── mobile-vision-android-main/  (or FallDetection/)
    ├── app/
    │   ├── src/
    │   │   ├── main/
    │   │   │   ├── java/edu/cs663/falldetect/
    │   │   │   ├── res/
    │   │   │   └── assets/
    │   │   └── androidTest/
    │   └── build.gradle.kts
    ├── gradle/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradlew
    ├── gradlew.bat
    └── README.md
```

**Screenshot 1.2:** *(Directory view showing the extracted files)*

### Step 1.3: Open Project in Android Studio

1. Launch **Android Studio**
2. Click **"Open"** (or File → Open)
3. Navigate to your `temp` folder
4. Select the `mobile-vision-android-main` (or `FallDetection`) folder
5. Click **"OK"**
6. Wait for Gradle sync to complete (this may take 2-5 minutes on first open)
7. If prompted to update Gradle or plugins, click **"Don't remind me again"** or update if you prefer

**Screenshot 1.3:** *(Android Studio with the project open, showing the project structure)*

### Step 1.4: Run the Application

#### Option A: Using Android Studio Run Button (Recommended)

1. **Connect your Android device** via USB:
   - Enable **Developer Options** on your device (Settings → About Phone → Tap "Build Number" 7 times)
   - Enable **USB Debugging** (Settings → Developer Options → USB Debugging)
   - Connect device and allow USB debugging when prompted

2. **Select your device** from the device dropdown in Android Studio toolbar

3. **Click the green "Run" button** (▶️) or press `Shift + F10`

4. **Wait for build and installation** (first build may take 2-3 minutes)

5. **Grant permissions** when the app launches:
   - Camera permission (required for fall detection)
   - Location permission (required for emergency GPS)
   - Notification permission (Android 13+)

#### Option B: Using Command Line

```bash
cd temp/mobile-vision-android-main
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n edu.cs663.falldetect/.ui.MainActivity
```

**Screenshot 1.4:** *(App running on device showing the Home screen with camera preview)*

---

### Testing the Application

Once the app is running, follow these steps:

#### Test 1: Fall Detection
1. Go to **Home** tab
2. Click **"Start Monitoring"**
3. Point camera at yourself (full body visible)
4. Simulate a fall by quickly moving down
5. Emergency countdown should trigger if fall detected (probability > 85%)
6. Click **"Cancel"** to stop countdown or let it complete to see SMS composer

#### Test 2: Posture Monitoring
1. Go to **Home** tab
2. Click **"Start Monitoring"**
3. Slouch heavily and **hold position for 20+ seconds without moving**
4. You should see a warning snackbar after 15 seconds: "⚠️ Bad posture for 15s!"
5. Click **"Stop Monitoring"**
6. Go to **Posture** tab to see the saved analysis

#### Test 3: View Logs
1. Go to **Log** tab
2. View all monitoring sessions with timestamps, duration, and fall counts
3. Sessions with falls are highlighted in red

#### Test 4: Configure Settings
1. Go to **Settings** tab
2. Add up to 3 emergency contacts
3. Adjust countdown timer (10-30 seconds)
4. Toggle SMS and GPS features
5. Settings are automatically saved

---

## 🏗️ SECTION 2: ARCHITECTURE & TECHNICAL DETAILS

### Package Structure
```
edu.cs663.falldetect/
├── ui/
│   ├── MainActivity.kt           # Main activity with bottom navigation
│   ├── HomeFragment.kt           # Fall detection & posture monitoring
│   ├── SettingsFragment.kt       # Emergency contacts & settings
│   ├── LogFragment.kt            # Monitoring session history
│   ├── PostureFragment.kt        # Posture analysis history
│   ├── OverlayView.kt            # Pose skeleton overlay
│   └── EmergencyDialogFragment.kt # Full-screen countdown dialog
├── ml/
│   ├── FallDetector.kt           # BiLSTM model wrapper
│   ├── KeypointsBuffer.kt        # 30-frame sliding window
│   ├── YoloPoseEstimator.kt      # YOLO11n-Pose keypoint extraction
│   ├── GeminiPostureAnalyzer.kt  # Gemini AI posture analysis
│   └── PostureTracker.kt         # Time-based posture tracking
├── data/
│   ├── MonitoringSession.kt      # Session data model
│   ├── PostureAnalysis.kt        # Posture analysis data model
│   └── EmergencyContact.kt       # Contact data model
├── emergency/
│   └── EmergencyManager.kt       # SMS & GPS emergency system
└── util/
    └── FpsMeter.kt               # FPS counter for performance monitoring
```

### Key Components

#### 1. Fall Detection Pipeline
- **YoloPoseEstimator**: Extracts 17 COCO keypoints from camera frames using YOLO11n-Pose INT8 quantized model
- **KeypointsBuffer**: Maintains sliding window of 30 frames (34 values per frame = 17 keypoints × 2 coordinates)
- **FallDetector**: BiLSTM model analyzes temporal patterns, outputs fall probability (0.0-1.0)
- **Threshold**: Fall detected when probability > 0.85 (85%)

#### 2. Posture Monitoring System
- **GeminiPostureAnalyzer**:
  - Analyzes body angles (neck, spine, shoulder alignment)
  - Calls Gemini 2.0 Flash API with structured prompt
  - Falls back to local angle-based scoring if API fails
  - Scores posture 0-100 (EXCELLENT: 90-100, GOOD: 75-89, FAIR: 60-74, POOR: 0-59)
- **PostureTracker**:
  - Checks posture every 5 seconds during monitoring
  - Only saves analyses when bad posture (score < 70) sustained for 15+ seconds
  - Prevents false positives from temporary movements (bending to pick something up)

#### 3. Emergency Alert System
- **EmergencyDialogFragment**: Full-screen countdown with TTS announcements
- **EmergencyManager**:
  - Configurable countdown (10-30 seconds, default 15s)
  - Text-to-Speech: "Fall detected! Sending alert in X seconds"
  - Haptic feedback: Vibration pattern during countdown
  - SMS composer with GPS coordinates (user must press send)
  - Supports up to 3 emergency contacts

#### 4. Session Logging
- **MonitoringSession**: Tracks start time, duration, fall count
- **Persistence**: Gson serialization to SharedPreferences
- **UI**: RecyclerView with horizontal cards, red/green indicators

### Technical Specifications

#### Machine Learning Models
1. **YOLO11n-Pose** (`yolo11n-pose_float32.tflite`)
   - Input: 320×320 RGB image
   - Output: 17 keypoints (COCO format) with confidence scores
   - Quantization: INT8 for mobile optimization
   - Inference time: ~50-80ms on mid-range devices

2. **BiLSTM Fall Detector** (`fall_detection_model.tflite`)
   - Input: [1, 30, 34] (30 frames × 34 keypoint values)
   - Output: [1, 1] (fall probability)
   - Architecture: Bidirectional LSTM with Flex delegate
   - Inference time: ~20-30ms

3. **Gemini 2.0 Flash** (API-based)
   - Model: `gemini-2.0-flash-exp`
   - Input: Body angles + keypoint data
   - Output: Structured JSON with score, status, issues, recommendations
   - Latency: ~500-1000ms (network dependent)

#### Performance Optimizations
- Camera resolution: 640×480 for balance of quality and speed
- Frame processing: ~15-20 FPS (limited by ML inference)
- GPU delegate: Available but disabled (CPU sufficient for current models)
- Memory: Ring buffer prevents unbounded growth

### Permissions Required
- **CAMERA**: For pose detection and fall monitoring
- **ACCESS_FINE_LOCATION**: For emergency GPS coordinates
- **ACCESS_COARSE_LOCATION**: Fallback location permission
- **POST_NOTIFICATIONS**: For Android 13+ notification display
- **INTERNET**: For Gemini API calls (posture analysis)

---

## 📱 SECTION 3: FEATURES & FUNCTIONALITY

### ✅ Implemented Features

#### Home Screen (Fall Detection & Posture Monitoring)
- **Start/Stop Monitoring**: Single button to control all monitoring
- **CameraX Live Preview**: Real-time camera feed with portrait orientation lock
- **Pose Skeleton Overlay**: Visual feedback showing detected keypoints
- **FPS Counter**: Performance monitoring (top-left corner)
- **Fall Probability Display**: Real-time fall probability percentage
- **Auto Emergency Trigger**: Automatically shows countdown when fall detected (>85%)

#### Posture Tab
- **Summary Card**: Shows total analyses, average score, and breakdown by status
- **Detailed Analysis List**: RecyclerView of all posture checks with:
  - Timestamp
  - Score (0-100) with color coding
  - Status (EXCELLENT/GOOD/FAIR/POOR)
  - Specific issues detected
  - Personalized recommendations
  - Body angle measurements (neck, spine, shoulders)

#### Log Tab
- **Session History**: All monitoring sessions with:
  - Start timestamp
  - Duration (HH:MM:SS format)
  - Fall count with red/green indicators
  - Horizontal card layout for easy scanning

#### Settings Tab
- **Emergency Contacts**: Add up to 3 contacts with name and phone number
- **Countdown Timer**: Slider to adjust emergency countdown (10-30 seconds)
- **SMS Toggle**: Enable/disable SMS sending
- **GPS Toggle**: Enable/disable GPS coordinates in emergency messages
- **Auto-save**: All settings persist automatically

#### Emergency System
- **Full-Screen Dialog**: Immersive countdown experience
- **Text-to-Speech**: Voice announcements ("Fall detected! Sending alert in X seconds")
- **Haptic Feedback**: Vibration pattern during countdown
- **Cancel Button**: Large, easy-to-press cancel option
- **SMS Composer**: Pre-filled message with GPS coordinates (user confirms send)

### 🎯 Use Cases

#### Use Case 1: Elderly Person Living Alone
- App monitors for falls while person goes about daily activities
- If fall detected, countdown gives 15 seconds to cancel false alarm
- If no cancel, SMS sent to family members with GPS location
- Family can immediately call or send help

#### Use Case 2: Physical Therapy Patient
- Therapist monitors patient's posture during exercises
- App alerts when patient slouches for extended periods
- Posture history helps track improvement over time
- Recommendations guide patient to better form

#### Use Case 3: Office Worker
- App monitors posture during work hours
- Alerts when slouching at desk for 15+ seconds
- Prevents chronic back pain from poor sitting posture
- Weekly posture summary shows trends

---

## 🧪 SECTION 4: TESTING & VALIDATION

### Manual Testing Checklist

#### Basic Functionality
- [x] App launches to Home screen without crashes
- [x] Camera preview displays correctly in portrait mode
- [x] Start/Stop monitoring button works
- [x] Bottom navigation switches between all 4 tabs
- [x] All fragments load without crashes

#### Permissions
- [x] Camera permission requested on first monitoring
- [x] Location permission requested on first monitoring
- [x] Notification permission requested (Android 13+)
- [x] Rationale dialogs shown when permissions denied
- [x] App handles denied permissions gracefully

#### Fall Detection
- [x] YOLO model extracts keypoints from camera frames
- [x] Pose skeleton overlay displays on camera preview
- [x] BiLSTM model processes 30-frame sliding window
- [x] Fall probability updates in real-time
- [x] Emergency dialog triggers when probability > 85%
- [x] Countdown timer counts down correctly
- [x] TTS announces countdown
- [x] Cancel button stops emergency alert
- [x] SMS composer opens with pre-filled message

#### Posture Monitoring
- [x] Posture analyzed every 5 seconds during monitoring
- [x] Gemini API called successfully (or fallback used)
- [x] Only sustained bad posture (15+ seconds) is saved
- [x] Posture tab displays all saved analyses
- [x] Summary card shows correct statistics
- [x] Body angles calculated correctly
- [x] Recommendations are relevant and helpful

#### Session Logging
- [x] Sessions saved when monitoring stops
- [x] Log tab displays all sessions
- [x] Timestamps formatted correctly
- [x] Duration calculated accurately
- [x] Fall counts displayed with color indicators

#### Settings
- [x] Emergency contacts can be added/edited/deleted
- [x] Countdown slider updates value
- [x] SMS toggle works
- [x] GPS toggle works
- [x] Settings persist after app restart

### Known Issues & Limitations

1. **Gemini API Dependency**: Posture analysis requires internet connection and valid API key
2. **Camera Angle**: Fall detection works best when full body is visible in frame
3. **Lighting**: Poor lighting may affect keypoint detection accuracy
4. **Movement Speed**: Very slow falls may not trigger detection
5. **False Positives**: Quick movements (jumping, dancing) may trigger false alarms
6. **Posture Tracking**: User must hold bad posture for 15+ seconds to trigger warning

### Performance Metrics

- **Frame Rate**: 15-20 FPS during monitoring
- **Fall Detection Latency**: ~100-150ms from fall to detection
- **Posture Analysis Latency**: ~500-1000ms (Gemini API) or ~50ms (fallback)
- **Memory Usage**: ~150-200 MB during active monitoring
- **Battery Impact**: ~5-8% per hour of continuous monitoring

---

## 📦 SECTION 5: DEPENDENCIES & BUILD CONFIGURATION

### Core Dependencies
```gradle
// AndroidX Core
androidx.core:core-ktx:1.13.1
androidx.appcompat:appcompat:1.7.0
com.google.android.material:material:1.12.0
androidx.constraintlayout:constraintlayout:2.1.4

// Navigation
androidx.navigation:navigation-fragment-ktx:2.7.7
androidx.navigation:navigation-ui-ktx:2.7.7

// CameraX
androidx.camera:camera-core:1.3.4
androidx.camera:camera-camera2:1.3.4
androidx.camera:camera-lifecycle:1.3.4
androidx.camera:camera-view:1.3.4

// TensorFlow Lite
org.tensorflow:tensorflow-lite:2.14.0
org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0  // For Flex delegate

// Google Generative AI (Gemini)
com.google.ai.client.generativeai:generativeai:0.9.0

// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1

// Location Services
com.google.android.gms:play-services-location:21.3.0

// JSON Serialization
com.google.code.gson:gson:2.10.1
```

### Build Configuration

**File: `build.gradle.kts` (Project level)**
```kotlin
plugins {
    id("com.android.application") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

**File: `app/build.gradle.kts`**
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

### Asset Files Required

The following TFLite models must be present in `app/src/main/assets/`:

1. **`yolo11n-pose_float32.tflite`** (YOLO11n-Pose model)
   - Size: ~6 MB
   - Input: [1, 320, 320, 3] (RGB image)
   - Output: Keypoints with confidence scores

2. **`fall_detection_model.tflite`** (BiLSTM fall detector)
   - Size: ~2 MB
   - Input: [1, 30, 34] (30 frames of keypoints)
   - Output: [1, 1] (fall probability)

**Note:** These models are included in the repository and will be automatically packaged with the APK.

---

## 🔐 SECTION 6: API KEYS & CONFIGURATION

### Gemini API Key

The app uses Google's Gemini AI for posture analysis. The API key is currently hardcoded in `GeminiPostureAnalyzer.kt`:

```kotlin
val apiKey = "your api key"
```

**For Production:** Move this to `local.properties` or use BuildConfig:

1. Add to `local.properties`:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```

2. Update `app/build.gradle.kts`:
   ```kotlin
   android {
       defaultConfig {
           buildConfigField("String", "GEMINI_API_KEY", "\"${project.findProperty("GEMINI_API_KEY")}\"")
       }
   }
   ```

3. Use in code:
   ```kotlin
   val apiKey = BuildConfig.GEMINI_API_KEY
   ```

### Obtaining Your Own Gemini API Key

1. Go to https://makersuite.google.com/app/apikey
2. Click "Create API Key"
3. Copy the key and replace in the code

**Free Tier Limits:**
- 60 requests per minute
- 1,500 requests per day
- Sufficient for testing and personal use

---

## 📚 SECTION 7: TROUBLESHOOTING

### Common Issues

#### Issue 1: "Gradle sync failed"
**Solution:**
- Check internet connection
- File → Invalidate Caches → Invalidate and Restart
- Update Android Studio to latest version

#### Issue 2: "App crashes on launch"
**Solution:**
- Check logcat for error messages
- Ensure device has Android 8.0+ (API 26+)
- Grant all required permissions

#### Issue 3: "Camera preview is black"
**Solution:**
- Grant camera permission
- Ensure device camera is not in use by another app
- Try restarting the app

#### Issue 4: "Fall detection not working"
**Solution:**
- Ensure full body is visible in camera frame
- Check that YOLO model file exists in assets
- Verify FPS counter shows >10 FPS

#### Issue 5: "Posture analysis shows 'Using FALLBACK'"
**Solution:**
- Check internet connection
- Verify Gemini API key is valid
- Check API quota limits (60 requests/minute)

#### Issue 6: "Emergency SMS not sending"
**Solution:**
- Add at least one emergency contact in Settings
- Grant SMS permission when prompted
- Note: App opens SMS composer, user must press "Send"

### Debug Logging

Enable verbose logging by filtering logcat:

```bash
adb logcat -s FallDetect:* GeminiPostureAnalyzer:* YoloPoseEstimator:*
```

Look for these key log messages:
- `✅ Gemini API SUCCESS!` - Posture analysis working
- `Fall detected! Probability: X.XX` - Fall detection triggered
- `⚠️ Bad posture for 15s!` - Sustained bad posture detected

---
---

