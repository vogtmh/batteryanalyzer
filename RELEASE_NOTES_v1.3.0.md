# Battery Analyzer v1.3.0

## 🎉 What's New

### New Features
- **⚙️ Settings Activity**: Dedicated settings screen for managing app preferences
- **📝 Manual Design Capacity**: Set custom design capacity when not detected from logs
- **🔍 Smart Detection**: Automatic battery capacity detection using Android's PowerProfile API
- **ℹ️ About Screen**: View app version, build date, and feature list
- **📖 Help Center**: Built-in help documentation and troubleshooting guides
- **🎨 Modern Navigation**: Material Design drawer with edge-to-edge support

### Improvements
- **Intelligent Fallback Chain**: Automatically tries multiple sources for design capacity:
  1. Extract from dumpstate logs
  2. Use saved value from settings
  3. Query system PowerProfile API
  4. Prompt user for manual input
- **Persistent Settings**: All preferences saved using SharedPreferences
- **Better Error Handling**: More reliable parsing with proper null handling
- **Enhanced UI**: Cleaner interface with Material Design 3 components

### Bug Fixes
- Fixed design capacity synchronization between dialog and settings
- Fixed auto-detect file parsing to use saved design capacity
- Fixed parser to correctly identify unreliable design capacity values
- Improved edge-to-edge navigation with proper system insets

## 📦 Installation

Download `BatteryAnalyzer-v1.3.0.apk` and install on your Android device (Android 8.0+).

## 🔧 Usage

1. **Generate Dumpstate**: Dial `*#9900#` → "Run dumpstate/logcat"
2. **Open App**: Tap "Detect Files" or "Browse Files"
3. **Select File**: Choose dumpstate.txt from `/log` folder
4. **View Results**: See battery health, capacity, and cycle count

If design capacity isn't detected:
- App suggests a value from your device's system profile
- Enter manually or save in Settings for future use

## 📝 What's in the Release

- `BatteryAnalyzer-v1.3.0.apk` - Signed release APK
- `CHANGELOG.md` - Complete version history
- `README.md` - Updated documentation

## 🛠️ Technical Details

- **Version**: 1.3.0 (Build 4)
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Size**: 4.6 MB
- **Permissions**: None (uses Android file picker)
- **SHA-256**: `8a51ab4abb45e3037d0da07a6250e5e4e020e8097aeb798d8f6018717252c968`

## 📱 Compatibility

Tested on:
- Samsung Galaxy S24 Ultra (Android 14, OneUI 6)
- Should work on all Android 8.0+ devices

## 🔗 Links

- [Full Changelog](CHANGELOG.md)
- [Documentation](README.md)

## 🙏 Feedback

Found a bug or have a feature request? Please open an issue on GitHub!

---

**SHA-256 Checksum**: Run `shasum -a 256 BatteryAnalyzer-v1.3.0.apk` to verify integrity.
