# 🧭 Compass — Complete Setup Guide

## What This Guide Covers

1. Opening the project in Android Studio
2. Configuring Google Sign-In and Drive API
3. Generating your signing keystore
4. Building and running the APK
5. Feature activation checklist

---

## Prerequisites

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog 2023.1.1+ | developer.android.com/studio |
| JDK | 17 | bundled with Android Studio |
| Android SDK | API 34 | via Studio SDK Manager |
| Git | Any | git-scm.com |

---

## Step 1 — Open the Project

1. Launch Android Studio
2. **File → Open…** → select the `CompassApp/` folder
3. Wait for Gradle sync to complete (downloads ~200 MB of dependencies the first time)
4. If prompted to upgrade AGP, click **Don't remind me**

---

## Step 2 — Google Services Setup (for Drive sync & Sign-In)

### 2a. Create a Firebase / Google Cloud Project

1. Go to https://console.firebase.google.com
2. Click **Add project** → name it "Compass Diary"
3. Disable Google Analytics (optional)

### 2b. Register the Android App

1. In Firebase console → **Add app** → Android
2. Package name: `com.compass.diary`
3. App nickname: Compass
4. Debug SHA-1: run in terminal:
   ```bash
   cd android     # or from project root
   ./gradlew signingReport
   # Copy the SHA-1 from "debug" variant
   ```
5. Download `google-services.json`
6. Replace `app/google-services.json` with the downloaded file

### 2c. Enable Google Sign-In

1. Firebase console → **Authentication** → **Sign-in method**
2. Enable **Google**

### 2d. Enable Google Drive API

1. Go to https://console.cloud.google.com
2. Select your project
3. **APIs & Services → Library** → search "Google Drive API" → Enable
4. **APIs & Services → Credentials** → OAuth consent screen → fill in app name
5. Add scope: `https://www.googleapis.com/auth/drive.file`

---

## Step 3 — Anthropic API Key (AI Assistant)

1. Sign up / log in at https://console.anthropic.com
2. **API Keys → Create key**
3. Copy the key (starts with `sk-ant-…`)
4. In the app: **Settings → AI Assistant → API Key** → paste your key

The key is stored using Android Keystore encryption on the device.

---

## Step 4 — Build the APK

### Debug build (development)
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Release build (production)

First create a signing keystore:
```bash
keytool -genkey -v \
  -keystore compass-keystore.jks \
  -alias compass \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Add to `gradle.properties` (never commit this file!):
```properties
COMPASS_STORE_FILE=../compass-keystore.jks
COMPASS_STORE_PASSWORD=your_store_password
COMPASS_KEY_ALIAS=compass
COMPASS_KEY_PASSWORD=your_key_password
```

Add to `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(properties["COMPASS_STORE_FILE"] as String)
            storePassword = properties["COMPASS_STORE_PASSWORD"] as String
            keyAlias = properties["COMPASS_KEY_ALIAS"] as String
            keyPassword = properties["COMPASS_KEY_PASSWORD"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Then build:
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

---

## Step 5 — Install on Device

### Via ADB (USB)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Via Android Studio
Click the green **Run ▶** button with your device selected.

---

## Feature Activation Checklist

| Feature | Status | What's needed |
|---------|--------|---------------|
| Compass display | ✅ Ready | Nothing — uses hardware sensor |
| Compass lock unlock | ✅ Ready | Set during first-launch |
| Daily diary | ✅ Ready | Nothing |
| Rich text editor | ✅ Ready | Nothing |
| Drawing canvas | ✅ Ready | Nothing |
| Auto-save | ✅ Ready | Nothing |
| Full-text search | ✅ Ready | Nothing |
| Starred items | ✅ Ready | Nothing |
| Calendar view | ✅ Ready | Nothing |
| Version history | ✅ Ready | Nothing |
| Reminders | ✅ Ready | Nothing |
| Dark/light mode | ✅ Ready | Nothing |
| AES-256 encrypted DB | ✅ Ready | Nothing |
| AI Assistant | 🔑 Needs key | Add Anthropic API key in Settings |
| Google Drive sync | 🔑 Needs setup | Add google-services.json (Step 2) |
| Biometric unlock | 📱 Device-dependent | Phone must have fingerprint/face |
| Voice notes | ✅ Ready | Grant mic permission on first use |
| Image insertion | ✅ Ready | Grant storage permission on first use |
| Google Sign-In | 🔑 Needs setup | Complete Step 2 |

---

## Common Issues

### "Gradle sync failed"
- Make sure JDK 17 is selected in **File → Project Structure → SDK Location**
- Delete `.gradle/` folder and sync again

### "google-services.json not found"
- The placeholder file is intentional. Replace it with your real file from Firebase.

### "SQLCipher library failed to load"
- Ensure `CompassApplication.kt` calls `SQLiteDatabase.loadLibs(this)` in `onCreate()`
- This is already implemented in the provided code.

### "BiometricPrompt crashes on Android 8"
- BiometricPrompt requires Android 9 (API 28)+. The app silently disables the option on older versions.

### "Compass not working on emulator"
- Emulators don't have physical sensors. Use **Extended Controls → Virtual sensors** in AVD.
- Or test on a real device.

---

## Architecture Quick Reference

```
User taps compass → long-press → CompassViewModel (unlock FSM)
  → heading from CompassSensorManager (ROTATION_VECTOR sensor)
  → compares to saved angles in PreferencesManager (DataStore)
  → on match: navigate to DiaryHomeScreen

Diary edit → DailyPageScreen → AutoSaveManager (debounce 800ms)
  → DiaryRepository → Room (SQLCipher AES-256)
  → SyncWorker (WorkManager) → GoogleDriveRepository → Drive REST API

AI question → AIViewModel → DiaryRepository (all entries)
  → AnthropicApiService → claude-sonnet-4-20250514
  → response with source date citations
```
