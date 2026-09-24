# Build Spec: "Clipboard to PC" — Android → Windows Clipboard Bridge over Bluetooth HID

**Purpose of this document:** hand this to a coding agent as a single, self-contained brief to build a production-quality Android app in one pass. It defines the feature, the constraints that are non-negotiable (verified against current Android platform behavior), the architecture, the full file structure, and the documentation/testing bar expected of every file.

---

## 1. Product summary

**One-line pitch:** Copy anything on your Android phone, tap one button, and it appears typed out wherever your cursor is focused on Windows — no cable, no Windows software, no cloud.

**Core mechanism:** The app registers the phone as a Bluetooth **HID device** (keyboard role) and pairs directly with Windows, exactly like pairing a physical Bluetooth keyboard. When the user triggers a "send," the app reads the current clipboard text and **types it out** on the Windows PC by emitting standard HID keyboard reports over the existing Bluetooth connection.

> **Critical constraint the agent must design around:** Bluetooth HID has no concept of "clipboard" or "paste" — it only has keys. There is no Windows-side software to intercept a payload. **"Paste" in this app literally means "simulate keystrokes for every character in the clipboard, as fast as reliably possible."** This has real consequences (see §4) that must be reflected in the UI copy, the architecture, and the limitations screen — do not let the implementation imply a true clipboard-sync (like a shared OS clipboard) when it is actually a keystroke simulator.

---

## 2. Verified platform constraints (do not deviate from these without re-checking current docs)

1. **`android.bluetooth.BluetoothHidDevice`** is a public API since API 28 (Android 9). It requires `BLUETOOTH_CONNECT` at runtime for apps targeting API 31+. It is **not guaranteed to work on every device** — the underlying Bluetooth chipset/firmware must support the HID *device* role. This must be probed at runtime (`onAppStatusChanged(registered=false)` means unsupported) and surfaced to the user, not silently failed.
2. **Clipboard read is restricted since Android 10 (API 29):** an app can only read the system clipboard while it is the app with current input focus, or is the default IME. A background service **cannot** silently read clipboard contents on change. Design implication: do **not** build this around a passive `OnPrimaryClipChangedListener` running invisibly in the background — it will silently stop working on Android 10+ and produce a broken, hard-to-debug feature. Use the "trampoline" pattern in §5 instead.
3. **Android 14 (API 34) requires an explicit foreground service type.** Since this app holds a live Bluetooth connection, its foreground service must declare `android:foregroundServiceType="connectedDevice"` and the app must declare `FOREGROUND_SERVICE_CONNECTED_DEVICE` in the manifest, in addition to the base `FOREGROUND_SERVICE` permission.
4. **HID keystroke injection is limited to standard keyboard characters.** Reliable, layout-independent transmission covers ASCII letters, digits, and common punctuation via the standard USB HID keyboard usage table (plus Shift for uppercase/symbols). Extended Unicode (emoji, CJK, accented characters, etc.) has **no reliable, universal HID equivalent** — Windows' Alt+Numpad Unicode input works only in some contexts and depends on registry settings the app cannot control. This must be handled explicitly, not ignored (see §4.3).
5. **`POST_NOTIFICATIONS` runtime permission is required on API 33+** for the persistent connection-status notification.

---

## 3. Functional requirements

### 3.1 Must-have (v1)
- Pair phone with a Windows PC as a Bluetooth HID keyboard (reuse/extend the HID-device registration flow).
- Persistent connection-status notification (connected / disconnected / device name), with a "Send Clipboard" action button on it.
- In-app "Send Clipboard Now" button, usable any time the app is open.
- A Quick Settings Tile ("Send Clipboard") for one-tap sending without opening the app or notification shade.
- On trigger: read current clipboard text → convert to keystroke sequence → transmit over the active HID connection → show success/failure feedback (haptic + brief in-app/notification confirmation).
- Pairing/connection management screen: shows bonded devices, connection state, reconnect button, "forget device."
- Settings screen: typing speed (chars/sec), whether to auto-clear the app's own memory of the clipboard content after sending, whether to show the persistent notification, whether to skip/replace unsupported characters.
- First-run onboarding explaining: what permissions are needed and why, that clipboard content is only read at the moment the user triggers a send (never in the background), and that unsupported characters may be dropped.
- A "Limitations" info screen (see §4).

### 3.2 Should-have (v1.1, stub the architecture for it now)
- Clipboard **history** (last N sends) stored **encrypted, in-memory or in EncryptedSharedPreferences**, never in plain logs, with a user-controlled retention/clear-all.
- Support for BLE HID (HOGP) as a fallback transport for phones whose chipset doesn't support classic HID device role (architecture must keep the transport behind an interface — see §6 — so this can be added without touching the clipboard or keystroke-mapping layers).

### 3.3 Explicitly out of scope for v1 (state this to avoid scope creep)
- Sending images, files, or rich clipboard content (HID cannot transmit anything but keystrokes).
- True bidirectional clipboard sync (Windows → Android). One-directional only.
- Any background/passive clipboard capture. Every send is a deliberate user action.

---

## 4. The three limitations that must be designed for, not discovered later

### 4.1 Not every phone supports it
Detect at connection time; if `BluetoothHidDevice.registerApp()` reports `registered = false`, show a clear, specific error ("Your phone's Bluetooth hardware doesn't support acting as a keyboard") rather than a generic failure.

### 4.2 Clipboard can only be read at the moment of a deliberate, focused action
Implement clipboard reads only inside:
- The main Activity while it's resumed, **or**
- A transparent "trampoline" Activity (`Theme.Translucent.NoTitleBar`, no layout) launched by a notification action or Quick Settings Tile, which briefly takes input focus, reads the clipboard, hands off to the sending logic, and calls `finish()` immediately.
Never attempt this from a `BroadcastReceiver` or a bound/started `Service` with no window — it will silently return `null`/empty on Android 10+.

### 4.3 Character support is not universal
- Maintain an explicit `SupportedCharset` allow-list mapped 1:1 to HID usage codes (US QWERTY layout as the default assumption; note in settings that this assumes the Windows PC is also using a US layout, and flag that as a known limitation for other layouts).
- For characters outside the allow-list: per the user's setting, either **skip** (default) and report "N characters were skipped because they're not supported" or **substitute** with a configurable placeholder.
- Never silently drop characters without telling the user a drop occurred.

---

## 5. Architecture

**Pattern:** MVVM + Clean-Architecture-style layering, single-module Compose app (multi-module only if the agent judges the codebase will clearly exceed ~15k LOC; otherwise keep one `:app` module plus a `:core` module for the transport/keystroke logic to keep it testable in isolation).

```
UI (Compose) → ViewModel → UseCase/Interactor → Repository → DataSource
                                              ↘ HidTransport (interface) → BluetoothHidTransport (impl)
```

**Key seams (interfaces), so implementations are swappable and independently testable:**
- `HidTransport` — `connect()`, `disconnect()`, `sendText(String)`, `connectionState: Flow<ConnectionState>`. Implemented first by `BluetoothClassicHidTransport`; leaves room for a future `BleHidTransport`.
- `ClipboardReader` — `readCurrentClipboard(): ClipboardResult`. Implemented by `FocusedClipboardReader` (only callable from a focused Activity context).
- `TextToKeystrokes` — pure function `String -> List<KeyReport>`, fully unit-testable with no Android dependencies (this is the highest-value class to get 100% unit test coverage on).

**Dependency injection:** Hilt.
**Concurrency:** Kotlin Coroutines + Flow throughout; no raw threads/callbacks in app code.
**Persistence:** DataStore (Preferences) for settings; EncryptedSharedPreferences (androidx.security.crypto) only if/when history (§3.2) is implemented — never plain SharedPreferences for anything clipboard-derived.

---

## 6. Full file structure with per-file purpose

Every file below must open with a doc-comment header (see §8 for the exact required format). Package root: `com.yourdomain.clipboardtopc` (agent: substitute the real applicationId).

```
app/
├── build.gradle.kts                         # App module config: compileSdk, minSdk=28, targetSdk=latest stable,
│                                             #   Compose, Hilt, versionCode/versionName scheme (see §11)
├── src/main/AndroidManifest.xml              # All permissions (see §7), foreground service type declaration,
│                                             #   trampoline activity declaration (excludeFromRecents, noHistory,
│                                             #   theme=Translucent), Quick Settings Tile service declaration
│
├── src/main/kotlin/.../
│   ├── ClipboardToPcApplication.kt           # @HiltAndroidApp Application class; app-wide init (Timber logging
│                                             #   setup with a redaction tree — see §9)
│   │
│   ├── di/
│   │   ├── TransportModule.kt                # Binds HidTransport -> BluetoothClassicHidTransport
│   │   ├── ClipboardModule.kt                # Binds ClipboardReader -> FocusedClipboardReader
│   │   └── DataStoreModule.kt                # Provides DataStore<Preferences> singleton
│   │
│   ├── hid/
│   │   ├── HidTransport.kt                   # Interface (see §6 seams)
│   │   ├── BluetoothClassicHidTransport.kt   # Implements registerApp/sendReport, exposes ConnectionState Flow,
│   │   │                                     #   handles onAppStatusChanged/onConnectionStateChanged, reconnect logic
│   │   ├── HidReportDescriptor.kt            # The combo keyboard+mouse (keyboard-only reports used here) byte[]
│   │   │                                     #   descriptor + SDP settings builder
│   │   ├── KeyReport.kt                      # Data class: modifier byte + keycode byte(s), 8-byte boot report
│   │   ├── UsKeyboardMap.kt                  # char -> (modifier, keycode) lookup table for supported charset
│   │   ├── TextToKeystrokes.kt               # Pure mapping function String -> List<KeyReport>; returns
│   │   │                                     #   (List<KeyReport>, droppedCharCount) — no Android imports, 100% unit tested
│   │   └── ConnectionState.kt                # Sealed class: Disconnected / Connecting / Connected(deviceName) / Unsupported
│   │
│   ├── clipboard/
│   │   ├── ClipboardReader.kt                # Interface
│   │   ├── FocusedClipboardReader.kt         # Reads ClipboardManager only when called with a resumed/focused context
│   │   ├── ClipboardResult.kt                # Sealed class: Text(String) / Empty / Sensitive (flagged via
│   │   │                                     #   ClipData.Description.EXTRA_IS_SENSITIVE on API 33+, handled per §9) / Unreadable
│   │   └── SendClipboardUseCase.kt           # Orchestrates: read -> map to keystrokes -> transmit -> emit result
│   │
│   ├── ui/
│   │   ├── MainActivity.kt                   # Compose host, connection status screen, "Send Now" button
│   │   ├── TrampolineActivity.kt             # No-UI activity: gains focus, triggers SendClipboardUseCase, finishes
│   │   ├── theme/ (Theme.kt, Color.kt, Type.kt)   # Material 3 theming, dynamic color support, dark mode
│   │   ├── pairing/PairingScreen.kt          # Bonded devices list, connect/forget, live connection state
│   │   ├── settings/SettingsScreen.kt        # Typing speed, notification toggle, unsupported-char behavior
│   │   ├── onboarding/OnboardingScreen.kt    # Permission rationale + how clipboard access actually works
│   │   └── limitations/LimitationsScreen.kt  # Plain-language explanation of §4 constraints
│   │
│   ├── viewmodel/
│   │   ├── MainViewModel.kt
│   │   ├── PairingViewModel.kt
│   │   └── SettingsViewModel.kt
│   │
│   ├── service/
│   │   ├── HidForegroundService.kt           # foregroundServiceType="connectedDevice"; owns the live
│   │   │                                     #   BluetoothClassicHidTransport instance; posts/updates the
│   │   │                                     #   persistent notification with the "Send Clipboard" action
│   │   └── SendClipboardTileService.kt       # Quick Settings TileService; launches TrampolineActivity
│   │
│   ├── notification/
│   │   └── HidNotificationBuilder.kt         # Builds/updates the connection-status notification + action intent
│   │
│   └── data/settings/
│       ├── AppSettings.kt                    # Data class mirroring DataStore-backed preferences
│       └── SettingsRepository.kt             # Read/write settings via DataStore
│
├── src/test/kotlin/.../                      # Pure JVM unit tests (no Android framework, run on every commit)
│   ├── hid/TextToKeystrokesTest.kt           # Exhaustive: every supported char, uppercase/shift handling,
│   │                                         #   unsupported-char drop counting, empty string, max-length input
│   └── hid/UsKeyboardMapTest.kt
│
├── src/androidTest/kotlin/.../                # Instrumented tests (need a device/emulator)
│   ├── clipboard/FocusedClipboardReaderTest.kt   # Verifies read succeeds when Activity is resumed
│   ├── ui/PairingScreenTest.kt               # Compose UI test: state -> rendered content
│   └── service/HidForegroundServiceTest.kt   # Service starts, posts notification, foregroundServiceType honored
│
├── proguard-rules.pro                        # R8 rules; keep HID callback classes reachable via reflection if any
└── src/main/res/                             # strings.xml (all user-facing text — no hardcoded strings in Kotlin),
                                              #   drawable icons, notification icon, Quick Settings tile icon

CHANGELOG.md                                  # Root-level. One dated entry per meaningful change, naming which
                                              #   files changed and why (see §8.3)
README.md                                     # Root-level. Setup, architecture diagram, how to run tests, known
                                              #   limitations (mirrors §4), permissions justification table
docs/
└── ARCHITECTURE.md                           # Expanded version of §5/§6 kept in sync with the actual code
```

---

## 7. Manifest / permissions (exact list, with justification comments required inline)

```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" /> <!-- only if BLE fallback added -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<!-- Deliberately NO android.permission.INTERNET anywhere in this app — this is a selling point
     (clipboard content never leaves the device except over the direct Bluetooth link) and must
     be verified by checking the merged manifest before release. -->
```

Every `<uses-permission>` must have a one-line XML comment explaining why, and the same explanation must appear in the onboarding screen and the README's permissions table — keep all three in sync.

---

## 8. Documentation standard (applies to every file — this was explicitly requested)

### 8.1 File header (required at the top of every Kotlin file)
```kotlin
/**
 * [ClassName] — [one-line purpose]
 *
 * Responsibility: [what this file owns, in 1–2 sentences]
 * Depends on: [key collaborators, if any]
 * Notes: [any non-obvious constraint, e.g. "must only be called from a focused Activity context — see §4.2 of build spec"]
 */
```

### 8.2 Function-level KDoc
Every public function/class gets a KDoc block: purpose, `@param`, `@return`, and — critically for this app — any platform constraint it exists to satisfy (cite the constraint, e.g. "Android 10+ clipboard focus restriction").

### 8.3 CHANGELOG.md discipline
Every commit/change the agent makes must add a dated entry to `CHANGELOG.md` in this format, listing every file touched:
```
## 2026-09-23
- Added `TextToKeystrokes.kt`, `UsKeyboardMapTest.kt`: initial char→HID keycode mapping with unit tests.
- Modified `BluetoothClassicHidTransport.kt`: added reconnect-with-backoff on unexpected disconnect.
```
This is mandatory, not optional — treat "document changes in each file" as: (a) header+KDoc in the file itself, and (b) a changelog entry every time the file is touched.

---

## 9. Security & privacy requirements (non-negotiable)

- **Never log clipboard content.** Configure logging (Timber or similar) with a custom Tree that redacts/omits any parameter tagged as clipboard-derived. Code review checklist item: no `Log.d`/`println` ever includes raw clipboard text, even in debug builds.
- **Respect `ClipData.Description.EXTRA_IS_SENSITIVE`** (API 33+): if the clipboard owner marked the content sensitive (e.g., a password manager), show a confirmation dialog ("This looks like sensitive content — send anyway?") rather than sending silently.
- **No network permission at all** (see §7) — this is both a privacy feature and something to state proudly in the store listing.
- If clipboard history (§3.2) is implemented, it must use `EncryptedSharedPreferences` or a SQLCipher-backed store, never plaintext, and must have a "clear all" that is reachable in at most one tap from settings.
- Add a `PRIVACY.md` describing exactly what data is read (clipboard, only on explicit trigger), what is transmitted (converted to keystrokes, direct Bluetooth link only), and what is stored (settings only, unless history is enabled).

---

## 10. Error handling & edge cases checklist

- Empty clipboard → clear "Nothing to send" feedback, not a silent no-op.
- Clipboard contains non-text content (image, URI, etc.) → explicit "Only text can be sent" message.
- Very long clipboard text → chunk transmission with the configured typing speed; show a progress indicator; allow cancel mid-send.
- Connection drops mid-send → stop cleanly, report how much was sent vs. not, don't crash, offer retry.
- Device doesn't support HID device role → detected at registration, surfaced with a specific explanatory message (§4.1), and the app should not pretend pairing succeeded.
- Bluetooth disabled → prompt to enable it, don't crash on a null adapter.
- Multiple bonded HID hosts → let the user pick which one is "active" rather than guessing.

---

## 11. Build / release requirements

- `minSdk = 28`, `targetSdk` = latest stable at build time, `compileSdk` matching.
- Semantic versioning for `versionName`; `versionCode` auto-incremented per release.
- R8/ProGuard enabled for release builds; verify HID-related classes and any reflection-accessed callback classes survive minification (add explicit `-keep` rules and note why in `proguard-rules.pro`).
- Signing config kept out of version control (placeholder + `local.properties`/env-var pattern, documented in README).
- Lint must pass with no suppressed warnings unless each suppression has an inline comment explaining why.
- CI (GitHub Actions or equivalent): run `./gradlew testDebugUnitTest lint` on every push; instrumented tests on a scheduled/PR-triggered emulator job.

---

## 12. Accessibility & localization

- All user-facing strings in `strings.xml` — zero hardcoded UI text in Kotlin.
- Content descriptions on every icon-only control (notification action, tile, connect/forget buttons).
- Minimum touch target sizes and color-contrast per Material 3 defaults — don't override without checking contrast ratios.
- Structure `strings.xml` so translation is a drop-in addition later, even if v1 ships English-only.

---

## 13. Suggested build order for the agent

1. Scaffold project (Hilt, Compose, module structure, manifest with all permissions + service declarations).
2. Implement `HidTransport` interface + `BluetoothClassicHidTransport` + `HidReportDescriptor`; get a hardcoded test string typing into Windows end-to-end before building any UI. **This is the highest-risk part — validate it first.**
3. Implement `TextToKeystrokes` + `UsKeyboardMap` with full unit test coverage (pure Kotlin, no emulator needed — fastest feedback loop).
4. Implement `FocusedClipboardReader` + `TrampolineActivity`; verify clipboard reads succeed via this path and fail (as expected) from a plain background service, to confirm the constraint in §4.2 is correctly designed around.
5. Implement `HidForegroundService` + notification with action button; wire up `SendClipboardTileService`.
6. Build UI screens (pairing, main, settings, onboarding, limitations) atop the now-working pipeline.
7. Add error handling/edge cases from §10.
8. Add security hardening from §9 (logging redaction, sensitive-clipboard confirmation).
9. Write remaining tests, finalize README/CHANGELOG/PRIVACY.md/ARCHITECTURE.md.
10. Accessibility pass, lint pass, ProGuard verification on a release build.

---

## 14. Definition of done

- A fresh clone builds and installs with no manual steps beyond standard Gradle sync.
- On a phone whose chipset supports HID device role: pairs with a Windows PC, and text copied on Android reliably appears typed on Windows via notification action, Quick Settings tile, and in-app button.
- Every file has the header + KDoc described in §8; `CHANGELOG.md` has an entry per meaningful change.
- Unit tests for `TextToKeystrokes`/`UsKeyboardMap` pass with edge cases covered; lint is clean; no clipboard content appears in any log output.
- README, PRIVACY.md, and ARCHITECTURE.md are present and accurate to the shipped code.
