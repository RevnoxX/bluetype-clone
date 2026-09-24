# BlueType — Clipboard to Windows PC via Bluetooth HID

BlueType turns your Android phone into a high-speed, direct Bluetooth HID keyboard for your Windows PC. Copy anything on Android, tap one button, and it is instantly typed into whatever window or application has focus on your PC — no cables, no Windows software required, and zero cloud dependency.

---

## 🎯 Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                               UI Layer (Jetpack Compose)                          |
|  - MainScreen (Connected device header, device switcher, nearby device list, FAB) |
|  - TrampolineActivity (Translucent, focused clipboard reader for system triggers) |
|  - Quick Settings Tile / Persistent Notification Action                          |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                                 ViewModel Layer                                   |
|  - MainViewModel (Coordinates UI state, transport state, scanning, and settings)  |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                                Domain & Use Cases                                 |
|  - SendClipboardUseCase (Validates connection, maps text, orchestrates dispatch)  |
|  - TextToKeystrokes (Pure JVM mapper from Unicode to HID usage codes)             |
|  - FocusedClipboardReader (Focus-guarded ClipboardManager reader)                 |
+------------------------------------------+----------------------------------------+
                                           |
                                           v
+-----------------------------------------------------------------------------------+
|                           Data & Hardware Transport Layer                         |
|  - HidTransport (Interface for Bluetooth Classic and future BLE HOGP)             |
|  - BluetoothClassicHidTransport (BluetoothHidDevice profile proxy & SDP registry)  |
|  - BluetoothScanner (Inquiry scanner for nearby Bluetooth hosts)                  |
|  - SettingsRepository (DataStore Preferences for persistent configuration)        |
+-----------------------------------------------------------------------------------+
```

---

## 🚀 Key Features

- **Direct Hardware Emulation:** Registers as a standard USB HID boot keyboard with Report ID 1.
- **Top Connected Header & Switcher:** Real-time connection badge with device name and 1-tap switcher to other devices.
- **Performant Device Discovery:** Rapid `LazyColumn` listing bonded devices and discovered nearby Bluetooth PCs.
- **Extended Floating Action Button:** 1-tap "Paste to PC" action with haptic feedback.
- **Quick Settings Tile:** Send clipboard text instantly without opening the app window.
- **Notification Action:** Persistent foreground connection notification with direct "Send Clipboard" action.
- **Zero Network Guarantee:** No `INTERNET` permission in the Android Manifest.

---

## 📋 Permissions Justification Table

| Permission | API Level | Justification |
| :--- | :--- | :--- |
| `BLUETOOTH_CONNECT` | 31+ | Required to register as an HID device and connect to the host Windows PC. |
| `BLUETOOTH_SCAN` | 31+ | Enables scanning for nearby Bluetooth host PCs (`neverForLocation` flag asserted). |
| `BLUETOOTH_ADVERTISE` | 31+ | Broadcasts HID keyboard SDP records to hosts. |
| `ACCESS_FINE_LOCATION` | 28–30 | Required for Bluetooth scanning on legacy Android versions (maxSdk=30). |
| `FOREGROUND_SERVICE` | 28+ | Keeps the Bluetooth HID connection alive when the app is in the background. |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | 34+ | Android 14+ requirement for foreground services maintaining active device connections. |
| `POST_NOTIFICATIONS` | 33+ | Displays persistent connection status and the one-tap "Send Clipboard" action. |
| `VIBRATE` | All | Provides immediate haptic confirmation when a send operation completes. |
| **`INTERNET`** | **None** | **Deliberately omitted.** Clipboard text never touches the network. |

---

## ⚠️ Known Platform Limitations (Spec §4)

1. **Keystroke Simulation, Not Shared Clipboard:**
   Bluetooth HID only understands keyboard reports. "Paste" simulates rapid keystrokes. Ensure your cursor is positioned in your target Windows app before triggering send.
2. **US QWERTY Layout Dependent:**
   HID keycodes assume the host PC uses a standard US layout. Non-ASCII characters (emoji, accented glyphs) are either skipped with a warning or replaced by a fallback placeholder.
3. **Hardware Chipset Support:**
   The phone's Bluetooth controller firmware must support the HID Device role (`BluetoothHidDevice`). If unsupported, BlueType reports this clearly rather than silently failing.
4. **Android 10+ Input Focus:**
   The OS prevents background services from silently reading clipboard contents. BlueType uses a translucent `TrampolineActivity` to briefly take input focus and send text cleanly.

---

## 🧪 Running Unit Tests

Unit tests are 100% pure JVM tests and can be executed without an emulator:

```bash
# Set Java 17+ / Android Studio JBR
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Run tests
.\gradlew.bat testDebugUnitTest
```

---

## 📦 Building the APK

```bash
.\gradlew.bat assembleDebug
```
The resulting APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
