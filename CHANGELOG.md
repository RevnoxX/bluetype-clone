# Changelog

All notable changes to the BlueType (Clipboard to PC) project will be documented in this file.

## 2026-09-24
- Added `KeyReport.kt`: Standard USB HID 8-byte boot keyboard report data class with modifier bitmasks.
- Added `UsKeyboardMap.kt`: Char-to-HID usage code lookup table for US QWERTY layout.
- Added `TextToKeystrokes.kt`: Pure converter from text to HID key reports with newline normalization and unsupported character handling.
- Added `UsKeyboardMapTest.kt`: Pure JVM unit tests verifying US keyboard mapping and unsupported character handling.
- Added `TextToKeystrokesTest.kt`: Pure JVM unit tests verifying key report generation, empty key release alternation, and character drop policies.
- Configured Gradle dependencies in `app/build.gradle.kts` and `gradle/libs.versions.toml`: minSdk=28, DataStore, Material 3, Timber, Coroutines test.
