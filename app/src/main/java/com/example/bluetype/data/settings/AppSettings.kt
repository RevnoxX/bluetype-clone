package com.example.bluetype.data.settings

/**
 * AppSettings — Data class representing user-configurable application preferences.
 *
 * Responsibility: Encapsulates typing speed, notification, and character translation settings.
 * Depends on: None.
 * Notes: Mirrored to DataStore preferences for persistent storage (§6).
 */
data class AppSettings(
    val typingDelayMs: Long = 10L,
    val skipUnsupportedChars: Boolean = true,
    val unsupportedCharReplacement: String = "?",
    val showPersistentNotification: Boolean = true,
    val autoClearClipboard: Boolean = false,
    val activeDeviceAddress: String? = null
)
