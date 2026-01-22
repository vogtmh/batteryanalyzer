# Battery Analyzer v1.2.0

## What's New in v1.2.0

### New Features
- ✨ **Manual Design Capacity Entry** - When design capacity cannot be determined from the log file (e.g., firmware bugs), the app now prompts you to:
  - Enter the design capacity manually if you know it
  - Look it up online via Google search
  - Recalculate battery health with the correct value
- 🔍 **Smart Detection** - Automatically detects when design capacity data is unreliable or missing
- 📱 **Device-Specific Search** - Online lookup includes your device model for better search results

### Improvements
- 🎯 Better user experience for devices with invalid firmware data (like CMF Phone reporting 499 mAh instead of 5000 mAh)
- 💡 Input validation ensures design capacity is within reasonable range (2000-15000 mAh)
- 🔄 Dialog can be re-opened after looking up specs online

---

# Battery Analyzer v1.1.0

## What's New in v1.1.0

### Bug Fixes
- ✅ Fixed HMD devices showing incorrect current capacity (was using instantaneous charge instead of learned capacity)
- ✅ Fixed Nothing Phone devices with invalid design capacity firmware bug (now uses healthd full charge as fallback)
- ✅ Added support for battery stats "Estimated battery capacity" and "Last learned battery capacity"
- ✅ Improved parsing priority: Samsung CAP_NOM takes precedence over battery stats learned capacity
- ✅ Fixed battery health percentage display (restored text color, removed background)

### Improvements
- 📊 Better handling of devices with missing or invalid design capacity data
- 🔧 More robust capacity detection across different Android devices
- 🎯 Selective use of battery stats data (only when manufacturer-specific data unavailable)

---

# Battery Analyzer v1.0.0

## Features

### Multi-Device Support
- ✅ Samsung devices (*#9900# dumpstate files)
- ✅ Sony devices (bug report ZIP files)
- ✅ Other Android devices (generic bug reports)

### Battery Analysis
- **Battery Health**: Current capacity vs design capacity percentage
- **Cycle Count**: Number of charge cycles
- **State of Charge**: Current battery level
- **First Use Date**: When battery was first used (Samsung only)
- **Capacity Loss**: How much capacity has degraded
- **Design Capacity**: Original battery capacity (2000-15000 mAh range supported)

### UI Features
- 🔍 **Auto-detect** dumpstate/bugreport files on device
- 📁 **Manual browse** for file selection
- 📱 **ZIP file support** - automatically extracts Sony bug reports
- 🎨 **Dark mode support** with vibrant color-coded health status
- 🔄 **Rotation handling** - preserves state during screen rotation
- 📊 **Device information** - Shows brand and model
- ℹ️ **Help dialog** - Instructions for Samsung and Sony devices

### Technical
- Parses large files (100-200 MB) efficiently
- Supports both TXT and ZIP formats
- Automatic permission handling
- Blue Material Design 3 theme

## Installation

**Option 1: Direct APK Install**
1. Download `BatteryAnalyzer-v1.0.0.apk` below
2. Enable "Install from unknown sources" in Android settings
3. Open the APK and install

**Option 2: ADB Install**
```bash
adb install BatteryAnalyzer-v1.0.0.apk
```

## How to Use

### Samsung Devices:
1. Open Phone app
2. Dial `*#9900#`
3. Tap "Run dumpstate/logcat"
4. Wait for completion
5. Open Battery Analyzer and tap "Detect Files"

### Sony Devices:
1. Enable Developer Options (tap Build Number 7 times)
2. Go to Settings → System → Developer options
3. Tap "Take bug report" → "Full report"
4. Wait for notification
5. Open Battery Analyzer and browse for the ZIP file

## Requirements
- Android 8.0 (API 26) or higher
- Storage permission for file detection

## Known Limitations
- First use date only available on Samsung devices
- Design capacity must be between 2000-15000 mAh
- Requires storage access permission

## Support
Report issues: https://github.com/vogtmh/batteryanalyzer/issues
