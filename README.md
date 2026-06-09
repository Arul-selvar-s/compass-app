# 🧭 Compass — Private Diary App

A premium Android diary application disguised as a compass.

## Project Setup

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34

### Step 1 — Clone & Open
Open this folder in Android Studio as an existing project.

### Step 2 — Google Services
1. Create a project at https://console.firebase.google.com
2. Enable Google Sign-In authentication
3. Enable Google Drive API at https://console.cloud.google.com
4. Download `google-services.json` and place it in `app/`

### Step 3 — Anthropic API Key (optional for AI features)
- Obtain a key from https://console.anthropic.com
- Set it in the app via Settings → AI Assistant → API Key
- The key is stored encrypted on-device

### Step 4 — Build & Run
```bash
./gradlew assembleDebug
# or use Android Studio Run button
```

## Architecture

```
com.compass.diary/
├── data/
│   ├── local/          # Room + SQLCipher encrypted database
│   │   ├── dao/        # Data Access Objects
│   │   ├── database/   # AppDatabase (AES-256 encrypted)
│   │   └── entity/     # Room entities
│   ├── remote/         # Anthropic API client
│   └── repository/     # Single source of truth
├── ui/
│   ├── components/     # DrawingCanvas, RichTextEditor
│   ├── navigation/     # NavGraph with all routes
│   ├── screens/        # All 12 screens
│   └── theme/          # Material 3 dark/light theme
├── util/               # Sensors, AutoSave, Preferences
└── viewmodel/          # MVVM ViewModels
```

## Feature Map

| Feature | Status | Notes |
|---------|--------|-------|
| Compass display | ✅ | Real sensor, smooth animation |
| Compass unlock | ✅ | 2-direction lock sequence |
| Auto-save | ✅ | Debounced, every keystroke |
| Rich text editor | ✅ | Bold/italic/underline/lists |
| Drawing canvas | ✅ | 4 tools, 10 colours, undo/redo |
| Encrypted DB | ✅ | SQLCipher AES-256 |
| Starred items | ✅ | Star any text selection |
| Full-text search | ✅ | Across all diary pages |
| AI Assistant | ✅ | Anthropic Claude integration |
| Calendar view | ✅ | With entry highlights |
| Version history | ✅ | Per-page save history |
| Reminders | ✅ | In-app, optional notifications |
| Dark/light mode | ✅ | System, dark, or light |
| Google Drive sync | 🔧 | Requires google-services.json |
| Biometric unlock | 🔧 | Requires device with biometrics |
| Voice notes | 🔧 | Scaffold in place, needs MediaRecorder wiring |
| Image insertion | 🔧 | Needs Gallery/Camera picker |

## Security

- All diary data encrypted with SQLCipher (AES-256)
- Compass lock: 2-direction sequence (±20° tolerance)
- Optional biometric fallback
- No analytics, no ads, no tracking
- Google Drive sync uses OAuth 2.0

## Customisation

### Change the unlock tolerance
In `PreferencesManager`, adjust `KEY_UNLOCK_TOLERANCE` default from `20f` to a tighter/looser value.

### Change AI model
In `AnthropicApiService.kt`, change `MODEL` to any Claude model you have access to.

### Add more drawing colours
In `DrawingCanvas.kt`, extend the `palette` list.
