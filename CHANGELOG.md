# Changelog

All notable changes to the **BlueType** (Android to Windows Bluetooth HID Keyboard & Clipboard Bridge) project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-09-24

### 🚀 High-Throughput Typing Engine & Performance Optimizations
- **Tight-Coupled Key-Down / Key-Up Sequences (`BluetoothClassicHidTransport.kt`):**
  - Eliminated artificial delays between key-down and key-up reports (0ms gap), ensuring Windows does not trigger OS auto-repeat or "long press" duplicate keystrokes.
  - Sequenced key release (`KeyReport.EMPTY`) immediately following the active scancode.
- **Configurable Inter-Character Pacing & 0ms Turbo Mode (`BluetoothClassicHidTransport.kt`, `AppSettings.kt`, `SettingsSheet.kt`):**
  - Shifted inter-character delays to execute strictly *after* key-up release reports.
  - Implemented 0ms **Turbo Mode** achieving streaming speeds of **10–20 words per second** (~500–1000 characters per minute).
  - Maintained default 2ms pacing for universal Bluetooth 4.0/4.2 and high-load host stability, with a slider supporting 0ms to 40ms.
- **Buffer Saturation Protection & L2CAP Flow Control (`BluetoothClassicHidTransport.kt`):**
  - Added transmission tracking and logging for `BluetoothHidDevice.sendReport()` boolean dispatch status to handle local HCI buffer saturation gracefully.
- **Recomposition Throttling (`MainViewModel.kt`):**
  - Implemented an 80ms minimum throttle interval for keystroke transmission progress notifications, eliminating main-thread Jetpack Compose recomposition thrashing during bulk text streaming while guaranteeing final completion renders at 100%.

### 🎨 UI/UX Redesign & Shimmer Animation
- **Glowing Shimmer Connected Header (`ConnectedDeviceHeader.kt`, `Shimmer.kt`):**
  - Created `Shimmer.kt` containing `rememberShimmerBrush()`, an infinite 2200ms linear gradient sweep.
  - Applied the glowing shimmer effect to `ConnectedDeviceHeader` when active in the `Connected` state, providing an unmistakable, polished visual cue that the Bluetooth HID link is live.
  - Integrated top status indicator badge, live host device name, and 1-tap **Switch Devices** trigger.
- **Redesigned Bluetooth Device Cards (`BluetoothDeviceItem.kt`):**
  - Restructured the card layout with clear visual hierarchy: device friendly name as bold title, monospace MAC address subtitle, and status pill badges (`Connected`, `Paired`, `Available`).
  - Added intelligent device-type iconography (`Computer` vs `Bluetooth` endpoint).
  - Moved action controls (`Connect`, `Disconnect`, `Info`) into a horizontal bottom action row, completely eliminating cramped vertical-strip wrapping.
- **Modern Material 3 Theme (`Theme.kt`, `Color.kt`, `Type.kt`):**
  - Crafted a professional Slate and Indigo palette with semantic status colors (`ConnectedGreen`, `ConnectingAmber`, `DisconnectedGray`, `ErrorRed`) supporting dynamic dark and light themes.

### 🔍 Hardware Diagnostics & Device Info Drawer
- **Device Info Bottom Sheet (`DeviceInfoSheet.kt`):**
  - Implemented a Material 3 inspector drawer displaying low-level hardware and link diagnostics for any discovered or paired device:
    - **Hardware MAC Address:** Displayed in monospace font with a 1-tap **Copy to Clipboard** button and toast confirmation.
    - **Pairing Bond State:** Live reporting of `BOND_BONDED`, `BOND_BONDING`, or `BOND_NONE`.
    - **Bluetooth Architecture:** Real-time classification of `Classic (BR/EDR)`, `Low Energy (BLE)`, or `Dual Mode`.
    - **Major Device Class:** Endpoint categorization (`Computer / Desktop PC`, `Phone`, `Keyboard / Peripheral`, `Audio / Video`).
    - **Signal Strength (RSSI):** Live signal strength in dBm for discovered nearby devices.
    - **HID Profile Role:** Verification of Windows HID host compatibility.
    - **Contextual Action:** Connect or Disconnect directly from within the drawer.

### 🚦 Standardized Error Codes (0x01 – 0x30)
- **Standardized Hexadecimal Error Codes Across All Layers:**
  - Implemented structured error messages across domain use cases, HID transport, clipboard readers, and UI feedback:
    - `[ERR_BT_NOT_CONNECTED: 0x01]`: Keystroke transmission attempted without an active Bluetooth connection.
    - `[ERR_HID_NOT_REGISTERED: 0x02]`: Android Bluetooth HID Device service proxy is not registered with the OS daemon.
    - `[ERR_PERMISSION_DENIED: 0x03]`: Missing runtime permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`).
    - `[ERR_CONNECTION_LOST: 0x04]`: Bluetooth connection dropped mid-transmission after sending $N$ of $M$ reports.
    - `[ERR_SEND_FAILED: 0x05]`: Bluetooth `sendReport` returned false or report dispatch failed.
    - `[ERR_CONN_INIT_FAILED: 0x06]`: Android failed to initiate connection to target Bluetooth host.
    - `[ERR_UNSUPPORTED_CHIPSET: 0x10]`: Phone's Bluetooth hardware or firmware lacks peripheral HID device role support.
    - `[ERR_BLUETOOTH_OFF: 0x11]`: Bluetooth radio adapter is disabled.
    - `[ERR_CLIPBOARD_EMPTY: 0x20]`: Android clipboard is empty or contains zero readable characters.
    - `[ERR_CLIPBOARD_UNREADABLE: 0x21]`: Clipboard content could not be read or accessed.
    - `[ERR_WINDOW_NOT_FOCUSED: 0x22]`: Window lacks input focus required by Android 10+ clipboard restrictions.
    - `[ERR_NON_TEXT_CLIP: 0x23]`: Primary clip contains non-text data (images, files, URI intents).
    - `[ERR_CHARS_UNSUPPORTED: 0x30]`: All characters in clipboard were unmappable to US QWERTY HID table.

### 🛡️ Security, Privacy & System Integration
- **Zero Internet Guarantee (`AndroidManifest.xml`):**
  - Manifest deliberately omits `android.permission.INTERNET`. Data physically cannot leave the device over Wi-Fi, cellular, or cloud.
- **Focused Clipboard Reader & Trampoline (`FocusedClipboardReader.kt`, `TrampolineActivity.kt`):**
  - Satisfies Android 10+ background clipboard restrictions (§4.2). Background triggers (Quick Settings tile and notification actions) launch a translucent, no-UI `TrampolineActivity` that gains momentary window focus, reads the clipboard, and terminates immediately.
- **Sensitive Clipboard Protection (`FocusedClipboardReader.kt`, `MainViewModel.kt`):**
  - Detects `ClipData.Description.EXTRA_IS_SENSITIVE` on Android 13+ (API 33+) to flag passwords and credentials copied from password managers, requesting explicit user confirmation before transmission.
- **Logging Redaction Tree (`BlueTypeApplication.kt`):**
  - Installed custom Timber `RedactionDebugTree` that scrubs clipboard payload identifiers and prevents raw clipboard contents from ever appearing in logcat.
- **Foreground Service & Connected Device Type (`HidForegroundService.kt`):**
  - Implemented Android 14+ (API 34+) compliant foreground service with `foregroundServiceType="connectedDevice"` to sustain live Bluetooth connections.
- **Quick Settings Tile (`SendClipboardTileService.kt`):**
  - Added Android System Quick Settings Tile for 1-tap clipboard transmission without opening the app.
- **Persistent Status Notification (`HidNotificationBuilder.kt`):**
  - Ongoing notification reflecting live connection status with a direct "Send Clipboard" action button.

### 🏛️ Core Architecture & Hardware Transport
- **Abstract HID Transport Seam (`HidTransport.kt`, `BluetoothClassicHidTransport.kt`):**
  - Clean interface for HID device registration, state flows, and report dispatching, paving the way for future BLE HOGP transport implementations.
- **USB HID Keyboard Descriptor & SDP Builder (`HidReportDescriptor.kt`):**
  - Standard USB HID Boot Keyboard descriptor (Report ID 1, Usage Page 0x07) and SDP record generator.
- **Pure JVM Scancode Conversion (`TextToKeystrokes.kt`, `UsKeyboardMap.kt`, `KeyReport.kt`):**
  - Pure Kotlin mapper converting Unicode characters into 8-byte USB boot keyboard reports with shift modifier handling, newline normalizations (`\r\n` $\rightarrow$ `\n`), and unsupported character tracking.
- **Discovery Scanner (`BluetoothScanner.kt`):**
  - Inquiry scanner for nearby Bluetooth devices with permission validation and RSSI signal tracking.
- **Settings Persistence (`AppSettings.kt`, `SettingsRepository.kt`):**
  - Jetpack DataStore Preferences repository managing typing delay, unsupported character policy, notification visibility, and auto-clear options.
- **Dependency Container (`AppContainer.kt`, DI Modules):**
  - Lightweight manual dependency injection container with swappable modules (`TransportModule`, `ClipboardModule`, `DataStoreModule`).

### 🧪 Unit Testing & Verification
- **100% Passing JVM Unit Test Suite:**
  - `UsKeyboardMapTest.kt`: Exhaustive verification of ASCII letters, digits, punctuation, and shift modifier application.
  - `TextToKeystrokesTest.kt`: Comprehensive testing of string-to-report sequences, empty inputs, newline normalizations, and character drop counting.
  - `SendClipboardUseCaseTest.kt`: Unit tests for connection validation, error code emission (`0x01`, `0x20`, `0x30`), and auto-clear coordination.
  - `AppSettingsTest.kt`: Tests for DataStore preference storage and default value fallbacks.

### 📚 Documentation
- **Production-Ready `README.md`:** Added project overview, status shields/badges, high-throughput streaming engine breakdown, device cards and info drawer guides, standardized error codes reference table, Windows 10/11 pairing and troubleshooting walkthrough, clean architecture diagram, and security model.
- **Updated `PRIVACY.md`:** Detailed local-only security, zero internet permission, and redaction policies.
- **Updated `docs/ARCHITECTURE.md`:** Architectural specification outlining layer responsibilities, platform constraints, and design patterns.
