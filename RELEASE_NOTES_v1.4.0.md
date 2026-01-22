# Release Notes - Version 1.4.0

**Release Date:** January 22, 2026

## New Features

### History Timeline Chart
- Added interactive battery health timeline chart at the top of history view
- Visualizes battery degradation over time with color-coded health indicators
- Shows trend indicator (▲/▼) with percentage change
- Displays health percentage labels on Y-axis and date labels on X-axis
- Automatically adapts colors for light and dark mode

### Improved History Display
- History items are now collapsed by default for cleaner interface
- Tap any history item to expand and view detailed battery information
- Added expand/collapse chevron indicator (▼/▲) that rotates smoothly
- Battery health percentage moved to subtitle for quick visibility
- Removed "Analyzed" timestamp, kept only logfile date in subtitle
- Individual delete button on each history entry

## Changes
- Removed "Clear All" button from history screen
- Simplified About dialog - removed build number display
- Timeline chart only appears when 2 or more history entries exist

## Technical Improvements
- Chart view dynamically adapts text and axis colors based on system theme
- Smooth animations for expand/collapse transitions (200ms duration)
- Optimized RecyclerView with multiple view types (chart + entries)
- Uses sealed classes for type-safe list items

## UI/UX Enhancements
- Chart displays as first item in history list (not overlay)
- Grid lines with percentage markers for easy reading
- Color-coded health visualization (green → red based on battery health)
- Responsive layout that works in both portrait and landscape modes

---

**APK File:** BatteryAnalyzer-v1.4.0.apk  
**Size:** 4.7 MB  
**Minimum Android Version:** 8.0 (API 26)  
**Target Android Version:** 14 (API 34)
