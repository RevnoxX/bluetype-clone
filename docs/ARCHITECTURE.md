# BlueType Architecture Specification

## Overview

BlueType is engineered with Clean Architecture and MVVM patterns in modern Jetpack Compose. It isolates pure domain mapping logic from Android Bluetooth framework APIs, ensuring complete unit testability and platform resilience.

---

## 1. Layers & Responsibilities

### UI Layer (`com.example.bluetype.ui`)
- **`MainActivity.kt`**: Single-activity Compose container. Owns runtime permission requests (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `POST_NOTIFICATIONS`) and sets up edge-to-edge window insets.
- **`MainScreen.kt`**: Primary Material 3 interface. Renders `ConnectedDeviceHeader`, device switcher, nearby Bluetooth devices `LazyColumn`, live keystroke transmission progress, and the Floating Action Button.
- **`TrampolineActivity.kt`**: Translucent (`Theme.Translucent.NoTitleBar`) activity designed to briefly take input focus to satisfy Android 10+ clipboard access restrictions when triggered from the notification action or Quick Settings tile. Finishes immediately after dispatching text.
- **`components/ConnectedDeviceHeader.kt`**: Header card displaying active PC status, 1-tap device switcher, disconnect trigger, and animated shimmer glow.
- **`components/BluetoothDeviceItem.kt`**: Redesigned device card item featuring title/MAC hierarchy, desktop/peripheral icons, status badges, and horizontal action bar (`Connect`, `Disconnect`, `Info`).
- **`components/DeviceInfoSheet.kt`**: Modal bottom sheet inspector displaying hardware MAC (with 1-tap copy), bond state, BT architecture (Classic/BLE/Dual), device class, RSSI in dBm, and HID host capability.
- **`components/Shimmer.kt`**: Infinite linear gradient sweep brush (`rememberShimmerBrush`) providing glowing visual feedback on active connected states.
- **`settings/SettingsSheet.kt`**: Modal bottom sheet for configuring typing delay (0ms Turbo to 40ms), unsupported character policy, notification visibility, and auto-clear.
- **`limitations/LimitationsDialog.kt`**: In-app educational dialog explaining HID constraints, keystroke simulation, and US QWERTY dependencies.
- **`onboarding/OnboardingSheet.kt`**: First-run permission primer and privacy rationale.

### ViewModel Layer (`com.example.bluetype.viewmodel`)
- **`MainViewModel.kt`**: Manages UI state (`MainUiState`), coordinates Bluetooth scanning, manages device connections, handles sensitive clipboard prompt dialogs, and throttles keystroke progress updates to an optimal 80ms interval.

### UseCase & Domain Layer (`com.example.bluetype.clipboard`)
- **`SendClipboardUseCase.kt`**: Coordinates reading clipboard via `ClipboardReader`, converting text to HID key reports via `TextToKeystrokes`, dispatching to `HidTransport`, and executing auto-clear logic.
- **`ClipboardReader.kt` & `FocusedClipboardReader.kt`**: Accesses `ClipboardManager` exclusively in focused window contexts, inspecting `ClipData.Description.EXTRA_IS_SENSITIVE`.
- **`ClipboardResult.kt`**: Sealed hierarchy (`Text`, `Sensitive`, `Empty`, `Unreadable`).

### HID Transport Layer (`com.example.bluetype.hid`)
- **`HidTransport.kt`**: Interface abstracting HID registration, state, and report transmission for Classic HID and future BLE HOGP transports.
- **`BluetoothClassicHidTransport.kt`**: Concrete implementation backed by Android `BluetoothHidDevice` and SDP registration. Features tight-coupling (0ms gap between key-down and key-up), configurable inter-character delays (0ms Turbo), and runtime chipset probing (`onAppStatusChanged(registered=false)`).
- **`HidReportDescriptor.kt`**: Standard USB HID keyboard descriptor (Report ID 1, Usage Page 0x07) and SDP record builder.
- **`KeyReport.kt`**: 8-byte boot keyboard report data structure (Modifier, Reserved, Keycodes 1..6).
- **`UsKeyboardMap.kt`**: Unicode-to-HID usage code lookup tables covering printable ASCII and Shift modifiers.
- **`TextToKeystrokes.kt`**: Pure function mapping arbitrary strings to alternating key-down and key-up reports with newline normalization and unsupported character tracking.
- **`BluetoothScanner.kt`**: Manages discovery of nearby Bluetooth host devices with RSSI tracking.

### Service Layer (`com.example.bluetype.service`)
- **`HidForegroundService.kt`**: Sustains the live Bluetooth HID connection in the background with `foregroundServiceType="connectedDevice"`.
- **`SendClipboardTileService.kt`**: Android Quick Settings Tile reflecting connection status and launching `TrampolineActivity`.

### Persistence Layer (`com.example.bluetype.data.settings`)
- **`AppSettings.kt`**: Data class representing user preferences.
- **`SettingsRepository.kt`**: AndroidX Jetpack DataStore Preferences repository with `SettingsProvider` abstraction.

---

## 2. Platform Constraints & Architectural Solutions

| Platform Constraint | Solution Pattern |
| :--- | :--- |
| **Android 10+ Clipboard Focus (§4.2)** | Clipboard reads are restricted to focused windows. Background triggers (notifications, QS tile) launch a translucent, no-UI `TrampolineActivity` which gains focus, reads clipboard, transmits, and finishes immediately. |
| **Android 14+ Foreground Service (§2.3)** | `HidForegroundService` declares `android:foregroundServiceType="connectedDevice"` and `FOREGROUND_SERVICE_CONNECTED_DEVICE` in manifest. |
| **Chipset HID Device Probing (§4.1)** | `BluetoothHidDevice.registerApp` return value and `onAppStatusChanged(registered)` determine whether phone hardware supports HID device role, emitting `[ERR_UNSUPPORTED_CHIPSET: 0x10]`. |
| **Zero Network Guarantee (§7)** | Manifest deliberately omits `android.permission.INTERNET`. Physical guarantee that clipboard data cannot leave the device. |
| **Logging Sanitization (§9)** | Installed custom `RedactionDebugTree` in Timber that scrubs clipboard payload identifiers and prevents raw clipboard contents from ever appearing in logcat. |
| **UI Recomposition Thrashing** | `MainViewModel` throttles keystroke progress updates to an 80ms minimum interval during high-speed typing to maintain 60/120 FPS UI smoothness. |
