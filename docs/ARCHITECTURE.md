# BlueType Architecture Specification

## Overview

BlueType is built with Clean Architecture and MVVM patterns in modern Jetpack Compose. It isolates pure domain mapping logic from Android Bluetooth framework APIs, ensuring testability and stability.

---

## 1. Layers & Responsibilities

### UI Layer (`com.example.bluetype.ui`)
- **`MainActivity.kt`**: Single-activity Compose container. Owns runtime permission requests (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `POST_NOTIFICATIONS`) and launches `MainScreen`.
- **`MainScreen.kt`**: Material 3 UI. Renders top connected PC card, device switcher, nearby Bluetooth devices `LazyColumn`, live keystroke transmission progress, and the Floating Action Button.
- **`TrampolineActivity.kt`**: Translucent (`Theme.Translucent.NoTitleBar`) activity designed to briefly take input focus to satisfy Android 10+ clipboard access restrictions when triggered from the notification or Quick Settings tile. Finishes immediately after dispatching text.
- **`components/ConnectedDeviceHeader.kt`**: Card rendering connected PC status and switch device trigger.
- **`components/BluetoothDeviceItem.kt`**: Item renderer for paired and discovered Bluetooth hosts.
- **`settings/SettingsSheet.kt`**: Modal bottom sheet for user preferences.
- **`limitations/LimitationsDialog.kt`**: In-app educational dialog on HID constraints.
- **`onboarding/OnboardingSheet.kt`**: Explains permission requirements.

### ViewModel Layer (`com.example.bluetype.viewmodel`)
- **`MainViewModel.kt`**: Manages UI state (`MainUiState`), coordinates scanning, handles device connection/disconnection, and manages sensitive clipboard prompts.

### UseCase & Domain Layer (`com.example.bluetype.clipboard`)
- **`SendClipboardUseCase.kt`**: Coordinates reading clipboard via `ClipboardReader`, converting text to HID key reports via `TextToKeystrokes`, and transmitting via `HidTransport`.
- **`ClipboardReader.kt` & `FocusedClipboardReader.kt`**: Accesses `ClipboardManager` only in focused window contexts, inspecting `EXTRA_IS_SENSITIVE`.
- **`ClipboardResult.kt`**: Sealed hierarchy (`Text`, `Sensitive`, `Empty`, `Unreadable`).

### HID Transport Layer (`com.example.bluetype.hid`)
- **`HidTransport.kt`**: Interface abstracting HID registration, state, and report transmission.
- **`BluetoothClassicHidTransport.kt`**: Concrete implementation backed by `BluetoothHidDevice` and SDP registration. Probes hardware support on `onAppStatusChanged(registered=false)` to surface chipset limitations.
- **`HidReportDescriptor.kt`**: Standard USB HID keyboard descriptor (Report ID 1) and SDP record configuration.
- **`KeyReport.kt`**: 8-byte boot keyboard report data structure.
- **`UsKeyboardMap.kt`**: Char-to-HID usage code lookup table.
- **`TextToKeystrokes.kt`**: Pure function mapping arbitrary strings to alternating key-down and key-up reports.
- **`BluetoothScanner.kt`**: Manages device discovery for nearby Bluetooth PCs.

### Service Layer (`com.example.bluetype.service`)
- **`HidForegroundService.kt`**: Keeps the HID connection alive in the background with `foregroundServiceType="connectedDevice"`.
- **`SendClipboardTileService.kt`**: Quick Settings Tile reflecting connection status and launching `TrampolineActivity`.

### Persistence Layer (`com.example.bluetype.data.settings`)
- **`AppSettings.kt`**: Data class representing user preferences.
- **`SettingsRepository.kt`**: DataStore Preferences repository.

---

## 2. Platform Constraints & Patterns

| Constraint | Solution Pattern |
| :--- | :--- |
| **Android 10+ Clipboard Focus** | Clipboard reads are restricted to focused windows. Background triggers (notifications, QS tile) launch a translucent `TrampolineActivity` which gains focus, reads clipboard, transmits, and finishes. |
| **Android 14+ Foreground Service** | `HidForegroundService` declares `android:foregroundServiceType="connectedDevice"` and `FOREGROUND_SERVICE_CONNECTED_DEVICE` in manifest. |
| **Chipset HID Device Probing** | `BluetoothHidDevice.registerApp` return value and `onAppStatusChanged(registered)` determine whether phone hardware supports HID device role. |
| **Zero Network Guarantee** | Manifest deliberately omits `INTERNET` permission. |
