# Changelog

All notable changes to the BlueType (Clipboard to PC) project will be documented in this file.

## 2026-09-24
- Added `ConnectionState.kt`: Sealed hierarchy modeling HID connection states (`Disconnected`, `Connecting`, `Connected`, `Unsupported`).
- Added `HidReportDescriptor.kt`: USB HID boot keyboard descriptor byte array with Report ID 1 and SDP settings builder.
- Added `HidTransport.kt`: Swappable interface defining contracts for Bluetooth HID device lifecycle and keystroke sending.
- Added `BluetoothClassicHidTransport.kt`: Production `BluetoothHidDevice` implementation with profile listener, SDP registration, runtime unsupported probing, and transmission chunking.
- Added `BluetoothScanner.kt`: Discovery manager for scanning nearby Bluetooth devices with `BLUETOOTH_SCAN` permission checks.
- Added `AppSettings.kt`: Preferences model for typing speed, notification, and character translation settings.
- Added `SettingsRepository.kt`: Jetpack DataStore-backed repository with `SettingsProvider` abstraction.
- Added `ClipboardResult.kt`: Sealed class modeling clipboard contents with Android 13+ `EXTRA_IS_SENSITIVE` detection.
- Added `ClipboardReader.kt` & `FocusedClipboardReader.kt`: Focus-guarded reader accessing `ClipboardManager` only in active window contexts (§4.2).
- Added `SendClipboardUseCase.kt`: Core business logic orchestrating conversion, hardware transmission, progress reporting, and auto-clearing.
- Added `HidNotificationBuilder.kt`: Notification helper constructing persistent status notifications with "Send Clipboard" action.
- Added `HidForegroundService.kt`: Android 14+ foreground service with `foregroundServiceType="connectedDevice"`.
- Added `SendClipboardTileService.kt`: System Quick Settings Tile launching `TrampolineActivity` for one-tap sending.
- Added `TrampolineActivity.kt`: Translucent no-UI activity used to briefly take input focus to read clipboard and finish.
- Added `AppContainer.kt`: Application dependency injection container.
- Added `TransportModule.kt`, `ClipboardModule.kt`, `DataStoreModule.kt`: Swappable DI module definitions.
- Added `BlueTypeApplication.kt`: Application entry point with Timber `RedactionDebugTree` preventing clipboard text logging (§9).
- Added `ConnectedDeviceHeader.kt`: Top card rendering active host PC status and device switcher per user prompt.
- Added `BluetoothDeviceItem.kt`: Performant list item for paired and nearby discovered Bluetooth devices.
- Added `SettingsSheet.kt`: Material 3 bottom sheet for configuring typing delay, unsupported character handling, and auto-clear.
- Added `LimitationsDialog.kt`: Educational dialog detailing keystroke simulation, US QWERTY assumptions, and chipset compatibility (§4).
- Added `OnboardingSheet.kt`: First-run permission rationale sheet for Bluetooth and notification permissions.
- Added `MainScreen.kt`: Material 3 clean minimal screen with top connected device header, device switcher, nearby device list, and clipboard FAB.
- Modified `MainActivity.kt`: Configured edge-to-edge Compose container, runtime permissions launcher, and DI wiring.
- Modified `Color.kt` & `Theme.kt`: Slate & modern accent color system for dark and light modes.
- Modified `strings.xml`: Externalized all UI text and accessibility content descriptions with zero hardcoded strings.
- Modified `AndroidManifest.xml`: Added Bluetooth Connect/Scan/Advertise, Foreground Service Connected Device, Post Notifications permissions with XML justifications, and declared services/activities.
- Added `app/proguard-rules.pro`: R8/ProGuard keep rules for HID callbacks, data classes, and services.
- Added `SendClipboardUseCaseTest.kt` & `AppSettingsTest.kt`: Pure JVM unit tests for use cases and preferences.
- Added `KeyReport.kt`, `UsKeyboardMap.kt`, `TextToKeystrokes.kt`: Core HID reports and US keyboard layout mapping.
- Added `UsKeyboardMapTest.kt`, `TextToKeystrokesTest.kt`: Exhaustive unit tests for keystroke conversion and character coverage.
- Configured Gradle dependencies in `app/build.gradle.kts` and `gradle/libs.versions.toml`: minSdk=28, DataStore, Material 3, Timber, Coroutines test.
- Added `README.md`, `PRIVACY.md`, `docs/ARCHITECTURE.md`.
