# Battery Analyzer App

An Android app that parses dumpstate files to extract and display battery health information.

## Features

- 📁 **File Selection**: Pick dumpstate.txt or bug report files from internal storage
- 🔋 **Battery Analysis**: 
  - Battery health percentage
  - Current capacity (mAh)
  - Design capacity (mAh)
  - Capacity loss
  - Cycle count
- ⚙️ **Settings**: Manage manual design capacity input
- 🔍 **Smart Detection**: Automatic design capacity from logs, settings, or system PowerProfile API
- 💡 **Manual Input**: Set custom design capacity for unsupported devices
- 📖 **Built-in Help**: Instructions and troubleshooting guides
- ℹ️ **About Screen**: View app version and features
- 🎨 **Color-coded Health Status**: Visual indicators for battery health
- 📱 **On-Device Processing**: No upload required, works offline

## How to Use

### Generate Dumpstate File
1. Open Phone Dialer
2. Dial `*#9900#`
3. Select "Run dumpstate/logcat"
4. Wait for completion
5. File is saved to `/log` folder in internal storage

### Analyze Battery Health
1. Open Battery Analyzer app
2. Tap "Detect Files" for auto-scan or "Browse Files" for manual selection
3. Navigate to `/log` folder (if browsing manually)
4. Select `dumpstate.txt` or bug report file
5. Wait for parsing to complete
6. View battery health results

### Manual Design Capacity
If design capacity cannot be determined from the log:
1. App will suggest a value from system PowerProfile (if available)
2. Enter design capacity manually in the dialog
3. Or set it later in Settings from the drawer menu
4. Saved capacity will be used automatically for future analyses

## Building the App

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK with API 34
- Or just the Android command-line tools

### Build Steps

#### Option 1: Android Studio
1. Open project in Android Studio
2. Wait for Gradle sync to complete
3. Connect Android device or start emulator
4. Click Run (▶️) or press Shift+F10

#### Option 2: Command Line
The project includes Gradle wrapper, so you don't need to install Gradle separately.

**Build Debug APK:**
```bash
cd /Users/maximilian.vogt/Documents/BatteryAnalyzerApp
./gradlew assembleDebug
```
APK will be in: `app/build/outputs/apk/debug/app-debug.apk`

**Install directly to connected device:**
```bash
./gradlew installDebug
```

**Build Release APK (Unsigned):**
```bash
./gradlew assembleRelease
```

### First-Time Setup
If you get "SDK location not found" error:
1. Install Android Studio or Android command-line tools
2. The build will automatically create `local.properties` pointing to:
   - macOS: `~/Library/Android/sdk`
   - Or set `ANDROID_HOME` environment variable

## Technical Details

### Minimum Requirements
- Android 8.0 (API 26) or higher
- Supports Android 14 (API 34)

### Parsed Battery Information
- **Cycle Count**: Extracted from `Cycle(X, Y)` pattern
- **Current Capacity**: From `CAP_NOM XmAh` pattern
- **Full Charge Capacity**: From `batteryFullChargeUah` (converted from µAh)
- **Design Capacity**: From `DesignCap` hex value

### Health Calculation
```
Health % = (Current Capacity / Design Capacity) × 100
```

### Health Status Thresholds
- **Excellent**: ≥ 95%
- **Good**: 85% - 94%
- **Fair**: 75% - 84%
- **Poor**: 65% - 74%
- **Replace Soon**: < 65%

## Project Structure
```
app/
├── src/main/
│   ├── java/com/example/batteryanalyzer/
│   │   ├── MainActivity.kt          # Main UI and file handling
│   │   ├── SettingsActivity.kt      # Settings management
│   │   ├── HelpActivity.kt          # Help and instructions
│   │   ├── AboutActivity.kt         # App information
│   │   ├── model/
│   │   │   └── BatteryInfo.kt       # Data models
│   │   └── parser/
│   │       └── DumpstateParser.kt   # Log file parser
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml    # Main UI layout
│   │   │   ├── activity_settings.xml
│   │   │   ├── activity_help.xml
│   │   │   └── activity_about.xml
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   └── drawable/
│   │       └── status_background.xml
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Permissions
This app requires **no special permissions**. It only accesses files selected by the user through the Android file picker.

## Compatibility
Tested on:
- Samsung Galaxy S24 Ultra (Android 14, OneUI 6)
- Should work on all Android devices running Android 8.0+

## Known Limitations
- Large files (50+ MB) may take 10-30 seconds to parse
- Battery information availability depends on device manufacturer's logging format
- Some devices may not generate dumpstate files through `*#9900#`

## Privacy
- All processing is done locally on device
- No internet connection required
- No data is uploaded or shared
- No analytics or tracking

## Troubleshooting

### File not found
- Ensure dumpstate was generated successfully
- Check `/log` folder in internal storage
- Try using a file manager app to locate the file

### No battery data found
- Dumpstate file may be incomplete
- Battery information format may vary by device
- Try generating a fresh dumpstate file

### App crashes on large files
- Close other apps to free up memory
- Try on a device with more RAM

## License
This project is open source and available for personal use.

## Credits
Created to help analyze battery health on Samsung Galaxy S24 Ultra and similar devices.
