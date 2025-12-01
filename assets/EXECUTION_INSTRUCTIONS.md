# SECTION 1: EXECUTION INSTRUCTIONS

## Fall Detection & Posture Monitoring Android Application
**Course:** CS663 Mobile Vision  
**Author:** Nikhil Chowdary  
**Repository:** https://github.com/nikhilc523/mobile-vision-android

---

## OVERVIEW

This document provides step-by-step instructions for downloading, extracting, opening, and running the Fall Detection Android application. Each step includes screenshots to verify successful execution.

---

## PREREQUISITES

Before you begin, ensure you have:

1. **Android Studio** (Latest version recommended - Arctic Fox or newer)
   - Download from: https://developer.android.com/studio
   
2. **Android Device or Emulator**
   - Physical device: Android 8.0 (API 26) or higher
   - USB debugging enabled (for physical device)
   - OR Android Emulator with API 26+

3. **System Requirements**
   - macOS, Windows, or Linux
   - At least 8GB RAM (16GB recommended)
   - 10GB free disk space

---

## STEP 1: DOWNLOAD CODE FROM CANVAS

### Instructions:

1. Log in to Canvas
2. Navigate to **CS663 → Assignments → Project 1**
3. Go to the **"Project 1 Turn In"** folder
4. Locate the submitted ZIP file: `FallDetection.zip` or `mobile-vision-android.zip`
5. Click **Download**

### Screenshot 1.1: Files Uploaded to Canvas

**TAKE THIS SCREENSHOT:**
- Show the Canvas page with the uploaded ZIP file
- Make sure the file name and upload date are visible
- Show the "Download" button or link

```
┌─────────────────────────────────────────────────────────────┐
│  Canvas - Project 1 Turn In Folder                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Files:                                                      │
│  📦 FallDetection.zip                    20 MB   Dec 1, 2024│
│     [Download]                                               │
│                                                              │
│  OR                                                          │
│                                                              │
│  📦 mobile-vision-android.zip            20 MB   Dec 1, 2024│
│     [Download]                                               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**PLACE SCREENSHOT 1.1 HERE**

---

## STEP 2: EXTRACT TO TEMP DIRECTORY

### Instructions:

1. Create a folder called **`temp`** on your Desktop or Documents folder
   - macOS: `~/Desktop/temp`
   - Windows: `C:\Users\YourName\Desktop\temp`
   - Linux: `~/Desktop/temp`

2. Move the downloaded ZIP file to the `temp` folder

3. Extract the ZIP file:
   - **macOS:** Double-click the ZIP file
   - **Windows:** Right-click → "Extract All..." → Choose `temp` folder
   - **Linux:** Right-click → "Extract Here"

4. After extraction, you should see a folder structure like this:

```
temp/
├── FallDetection/  (or mobile-vision-android/)
    ├── app/
    │   ├── src/
    │   │   ├── main/
    │   │   │   ├── java/
    │   │   │   │   └── edu/cs663/falldetect/
    │   │   │   ├── res/
    │   │   │   ├── assets/
    │   │   │   └── AndroidManifest.xml
    │   │   ├── androidTest/
    │   │   └── test/
    │   ├── build.gradle.kts
    │   └── proguard-rules.pro
    ├── gradle/
    │   └── wrapper/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    ├── gradlew
    ├── gradlew.bat
    └── README.md
```

### Screenshot 1.2: Directory View of Extracted Files

**TAKE THIS SCREENSHOT:**
- Open Finder (macOS) or File Explorer (Windows)
- Navigate to the `temp` directory
- Show the extracted folder with all files visible
- Use "List View" or "Details View" to show file structure
- Expand the main folder to show key subdirectories

**PLACE SCREENSHOT 1.2 HERE**

**What to show in the screenshot:**
- ✅ `app/` folder
- ✅ `gradle/` folder
- ✅ `build.gradle.kts` file
- ✅ `settings.gradle.kts` file
- ✅ `gradlew` and `gradlew.bat` files
- ✅ `README.md` file

---

## STEP 3: OPEN PROJECT IN ANDROID STUDIO

### Instructions:

1. **Launch Android Studio**
   - macOS: Open from Applications folder
   - Windows: Start Menu → Android Studio
   - Linux: Run `studio.sh` from installation directory

2. **Open the Project**
   - Click **"Open"** on the welcome screen
   - OR: File → Open (if Android Studio is already running)

3. **Navigate to the extracted folder**
   - Browse to: `temp/FallDetection/` (or `temp/mobile-vision-android/`)
   - Select the **root folder** (the one containing `build.gradle.kts`)
   - Click **"Open"**

4. **Wait for Gradle Sync**
   - Android Studio will automatically sync Gradle
   - This may take 2-5 minutes on first open
   - Wait for the message: **"Gradle sync finished"**
   - Check the bottom status bar - it should say "Ready"

5. **Verify Project Structure**
   - In the left panel, you should see:
     - `app/` module
     - `Gradle Scripts`
     - Project files

### Screenshot 1.3: Android Studio with Project Open

**TAKE THIS SCREENSHOT:**
- Show Android Studio with the project fully loaded
- Make sure the project structure is visible in the left panel
- Expand `app/src/main/java/edu/cs663/falldetect/` to show source files
- Show the Gradle sync is complete (no loading bars)
- Show the toolbar with the Run button (green ▶️)

**PLACE SCREENSHOT 1.3 HERE**

**What to show in the screenshot:**
- ✅ Project name in the title bar
- ✅ Project structure panel on the left
- ✅ Key folders expanded: `app/`, `java/`, `edu.cs663.falldetect/`
- ✅ Some key files visible: `MainActivity.kt`, `HomeFragment.kt`, etc.
- ✅ Gradle sync complete (status bar shows "Ready")
- ✅ Green Run button (▶️) in the toolbar

---

## STEP 4: RUN THE APPLICATION

### Instructions:

#### A. Connect Your Android Device (Physical Device)

1. **Enable Developer Options on your Android device:**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   - You'll see "You are now a developer!"

2. **Enable USB Debugging:**
   - Go to Settings → System → Developer Options
   - Enable "USB Debugging"

3. **Connect via USB:**
   - Connect your Android device to your computer via USB cable
   - On your device, tap "Allow" when prompted for USB debugging

4. **Verify Connection:**
   - In Android Studio, check the device dropdown (next to the Run button)
   - You should see your device name (e.g., "Pixel 5", "Samsung Galaxy S21")

#### B. Run the App

1. **Select Your Device:**
   - Click the device dropdown in the toolbar
   - Select your connected device

2. **Click the Run Button:**
   - Click the green **Run** button (▶️) in the toolbar
   - OR: Run → Run 'app'
   - OR: Press `Shift + F10` (Windows/Linux) or `Control + R` (macOS)

3. **Wait for Build and Installation:**
   - Gradle will build the APK (1-3 minutes on first build)
   - The APK will be installed on your device
   - The app will launch automatically

4. **Grant Permissions:**
   - When the app launches, it will request permissions:
     - **Camera** - Tap "Allow" (required for fall detection)
     - **Location** - Tap "Allow" (required for emergency GPS)
     - **SMS** - Tap "Allow" (optional, for emergency SMS)

5. **Verify App is Running:**
   - You should see the **Home** screen with:
     - Camera preview (live feed from your device camera)
     - "Start Monitoring" button
     - FPS counter in the top-left corner
     - Bottom navigation bar with 4 tabs: Home, Logs, Posture, Settings

### Screenshot 1.4: Application Running

**TAKE THIS SCREENSHOT:**
- Show the app running on your Android device
- Capture the Home screen with camera preview
- Make sure all UI elements are visible

**PLACE SCREENSHOT 1.4 HERE**

**What to show in the screenshot:**
- ✅ App title bar: "Fall Detection"
- ✅ Camera preview showing live feed
- ✅ "Start Monitoring" button (green)
- ✅ FPS counter (top-left corner)
- ✅ Fall probability display (should show "0.00" initially)
- ✅ Bottom navigation bar with 4 tabs: Home, Logs, Posture, Settings
- ✅ Current tab highlighted (Home)

---

## STEP 5: VERIFY ALL FEATURES

After the app is running, verify all features work correctly:

### 5.1 Test Fall Detection

1. Go to **Home** tab
2. Tap **"Start Monitoring"**
3. Point the camera at yourself (full body should be visible)
4. Wait 2-3 seconds for pose detection to initialize
5. Verify:
   - ✅ FPS counter shows 15-30 FPS
   - ✅ Fall probability updates in real-time
   - ✅ Button changes to "Stop Monitoring" (red)

6. Tap **"Stop Monitoring"** to stop

### 5.2 Test Posture Monitoring

1. Go to **Posture** tab
2. You should see:
   - Summary card showing total analyses and bad posture count
   - List of posture analyses (if you've run monitoring before)
3. Go back to **Home** tab
4. Start monitoring for 10-15 seconds
5. Return to **Posture** tab
6. Verify new posture analyses appear (one every 5 seconds)

### 5.3 Test Session Logging

1. Go to **Logs** tab
2. You should see:
   - List of monitoring sessions
   - Each session shows: time, duration, fall count
   - Green indicator (0 falls) or red indicator (falls detected)

### 5.4 Test Settings

1. Go to **Settings** tab
2. Add an emergency contact:
   - Tap "Add Contact" button
   - Enter name and phone number
   - Tap the checkmark (✓)
3. Adjust countdown timer (10-30 seconds)
4. Toggle SMS/GPS options
5. Verify settings are saved (close and reopen app)

---

## TROUBLESHOOTING

### Issue 1: Gradle Sync Failed

**Error:** "Gradle sync failed: Could not resolve dependencies"

**Solution:**
1. Check internet connection
2. File → Invalidate Caches → Invalidate and Restart
3. Try again after restart

### Issue 2: Device Not Detected

**Error:** No device appears in the device dropdown

**Solution:**
1. Verify USB debugging is enabled on device
2. Try a different USB cable
3. Restart Android Studio
4. Run `adb devices` in terminal to verify connection

### Issue 3: App Crashes on Launch

**Error:** App crashes immediately after launch

**Solution:**
1. Check logcat for error messages (View → Tool Windows → Logcat)
2. Verify device has Android 8.0+ (API 26+)
3. Grant all required permissions
4. Try uninstalling and reinstalling the app

### Issue 4: Camera Preview is Black

**Error:** Camera preview shows black screen

**Solution:**
1. Grant camera permission (Settings → Apps → Fall Detection → Permissions)
2. Ensure camera is not in use by another app
3. Restart the app
4. Try a different device

### Issue 5: Fall Detection Not Working

**Error:** Fall probability always shows 0.00

**Solution:**
1. Ensure full body is visible in camera frame
2. Check FPS counter - should be >10 FPS
3. Wait 2-3 seconds for model to initialize
4. Check logcat for errors: `adb logcat -s FallDetect:*`

---

## ADDITIONAL NOTES

### Build Configuration

- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Compile SDK:** 34
- **Build Tools:** 34.0.0
- **Gradle:** 8.5.0
- **Kotlin:** 1.9.24

### Required Permissions

The app requires the following permissions:
- `CAMERA` - For fall detection and posture monitoring
- `ACCESS_FINE_LOCATION` - For emergency GPS coordinates
- `ACCESS_COARSE_LOCATION` - For emergency GPS coordinates
- `SEND_SMS` - For emergency SMS (optional)
- `FOREGROUND_SERVICE` - For background monitoring
- `POST_NOTIFICATIONS` - For monitoring notifications (Android 13+)

### Asset Files

The following TFLite models are included in `app/src/main/assets/`:
- `yolo11n-pose_float32.tflite` (6.2 MB) - Pose estimation
- `fall_detection_model.tflite` (2.1 MB) - Fall detection
- `movenet_lightning_float32.tflite` (4.9 MB) - Backup pose model

### API Keys

The app uses Google Gemini API for posture analysis. The API key is included in the source code for testing purposes. For production use, you should obtain your own API key from: https://makersuite.google.com/app/apikey

---

## SUMMARY

You have successfully:
- ✅ Downloaded the code from Canvas
- ✅ Extracted the files to a temp directory
- ✅ Opened the project in Android Studio
- ✅ Built and ran the application on your device
- ✅ Verified all features are working

The Fall Detection app is now running and ready for testing!

---

**For questions or issues, please contact:**  
Nikhil Chowdary  
Email: [your-email@example.com]  
GitHub: https://github.com/nikhilc523/mobile-vision-android

---

**Document Version:** 1.0  
**Last Updated:** December 1, 2024

