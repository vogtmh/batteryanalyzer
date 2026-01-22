# Changelog

All notable changes to Battery Analyzer will be documented in this file.

## [1.3.0] - 2026-01-22

### Added
- **Settings Activity**: New dedicated settings screen for managing app preferences
- **Manual Design Capacity Input**: Set custom design capacity when not available in logs
- **PowerProfile API Integration**: Automatic battery capacity detection from system
- **About Activity**: View app version, build info, and features
- **Smart Design Capacity Flow**: Automatic fallback chain (log → settings → PowerProfile → user input)
- **Edge-to-Edge Navigation**: Modern drawer navigation with proper insets
- **Help Activity**: Built-in help and instructions

### Changed
- Improved navigation with drawer menu (Settings, Help, About)
- Enhanced settings persistence using SharedPreferences
- Better handling of missing design capacity data
- Cleaner UI with Material Design 3 components

### Fixed
- Design capacity now correctly syncs between dialog and settings
- Both manual file selection and auto-detect now use saved design capacity
- Fixed parser to return null when design capacity is unreliable

## [1.2.0] - 2025-12-15

### Added
- Initial public release
- Parse dumpstate files for battery information
- Display battery health percentage
- Show cycle count and capacity information
- Color-coded health status indicators
- Support for Samsung and Sony devices

### Technical
- Android 8.0+ (API 26) support
- Offline processing, no permissions required
- Material Design UI
