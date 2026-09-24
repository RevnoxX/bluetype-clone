# Privacy Policy for BlueType

BlueType is engineered with a strict **privacy-first, local-only architecture**.

---

## 1. Zero Network Access
- BlueType **does not request or declare** the `android.permission.INTERNET` permission in its manifest.
- It is physically impossible for the application to transmit data over Wi-Fi, cellular, or the internet.
- Your clipboard contents never touch a remote server, analytics provider, or cloud storage.

---

## 2. Clipboard Access
- **Explicit Trigger Only:** BlueType reads the Android system clipboard exclusively when you perform a deliberate action (tapping the in-app Floating Action Button, tapping the persistent notification action, or tapping the Quick Settings tile).
- **No Background Monitoring:** BlueType contains no passive background listeners or clipboard observers.
- **Sensitive Clipboard Protection:** On Android 13+ (API 33+), BlueType inspects `ClipData.Description.EXTRA_IS_SENSITIVE`. If password managers or secure apps flag content as sensitive, BlueType prompts you with an explicit confirmation dialog before transmitting.

---

## 3. Data Transmission
- Text read from the clipboard is translated into USB HID keyboard scancodes.
- Scancodes are emitted directly over the encrypted point-to-point Bluetooth connection to your paired Windows PC.
- No raw clipboard text is retained in persistent logs. Timber logging is configured with a redaction tree that actively omits clipboard-derived text even in debug builds.

---

## 4. Local Storage
- BlueType stores only non-sensitive app configuration in Android Jetpack DataStore Preferences (typing delay, notification preferences, unsupported character policies).
- No clipboard history is stored in plaintext.
