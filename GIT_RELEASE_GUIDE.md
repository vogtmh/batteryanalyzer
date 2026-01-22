# Git Commands for v1.3.0 Release

## Stage All Changes
```bash
cd /Users/maximilian.vogt/Documents/BatteryAnalyzerApp

# Stage modified files
git add app/build.gradle.kts
git add app/src/main/java/com/example/batteryanalyzer/MainActivity.kt
git add app/src/main/java/com/example/batteryanalyzer/AboutActivity.kt
git add app/src/main/java/com/example/batteryanalyzer/SettingsActivity.kt
git add app/src/main/java/com/example/batteryanalyzer/HelpActivity.kt
git add app/src/main/res/layout/activity_about.xml
git add app/src/main/res/layout/activity_settings.xml
git add app/src/main/res/layout/activity_help.xml
git add app/src/main/res/menu/main_menu.xml
git add app/src/main/res/values/strings.xml
git add app/src/main/AndroidManifest.xml

# Stage new documentation
git add README.md
git add CHANGELOG.md
git add RELEASE_NOTES_v1.3.0.md

# Stage release APK (optional - some prefer not to commit binaries)
git add app/build/outputs/apk/release/BatteryAnalyzer-v1.3.0.apk
```

## Commit Changes
```bash
git commit -m "Release v1.3.0: Settings, About, and Smart Design Capacity Detection

New Features:
- Add Settings activity for manual design capacity configuration
- Add About activity with version and build information
- Add Help activity with usage instructions
- Implement smart design capacity detection (logs → settings → PowerProfile → user)
- Add drawer navigation with edge-to-edge support
- Integrate PowerProfile API for automatic capacity detection

Improvements:
- Clean up debug output code
- Improve design capacity flow with fallback chain
- Better SharedPreferences integration
- Enhanced UI with Material Design 3

Bug Fixes:
- Fix design capacity sync between dialog and settings
- Fix auto-detect file parsing to use saved capacity
- Fix parser null handling for unreliable values

Documentation:
- Add CHANGELOG.md with version history
- Update README.md with new features
- Add RELEASE_NOTES_v1.3.0.md for GitHub release"
```

## Create Git Tag
```bash
git tag -a v1.3.0 -m "Battery Analyzer v1.3.0

What's New:
- Settings Activity with manual design capacity input
- About Activity with version information
- Smart design capacity detection with PowerProfile API
- Improved navigation and UI
- Bug fixes and stability improvements

Build Date: January 22, 2026
APK Size: 4.6 MB
SHA-256: 8a51ab4abb45e3037d0da07a6250e5e4e020e8097aeb798d8f6018717252c968"
```

## Push to GitHub
```bash
# Push commits
git push origin main

# Push tag
git push origin v1.3.0
```

## Create GitHub Release
1. Go to: https://github.com/YOUR_USERNAME/BatteryAnalyzerApp/releases/new
2. Select tag: `v1.3.0`
3. Release title: `Battery Analyzer v1.3.0`
4. Copy content from: `RELEASE_NOTES_v1.3.0.md`
5. Upload file: `app/build/outputs/apk/release/BatteryAnalyzer-v1.3.0.apk`
6. Mark as latest release
7. Publish release

## Verify
```bash
# Check status
git status

# View commit
git log -1

# View tag
git tag -l -n9 v1.3.0

# Verify remote
git remote -v
```

## Quick Release (All-in-One)
```bash
cd /Users/maximilian.vogt/Documents/BatteryAnalyzerApp

# Stage all changes
git add -A

# Commit
git commit -m "Release v1.3.0: Settings, About, and Smart Design Capacity Detection"

# Tag
git tag -a v1.3.0 -m "Battery Analyzer v1.3.0 - January 22, 2026"

# Push everything
git push origin main && git push origin v1.3.0

echo "✅ Release v1.3.0 pushed to GitHub!"
echo "📦 Create release at: https://github.com/YOUR_USERNAME/BatteryAnalyzerApp/releases/new"
```
