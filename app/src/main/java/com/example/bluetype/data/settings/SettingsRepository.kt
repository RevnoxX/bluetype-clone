package com.example.bluetype.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bluetype_settings")

/**
 * SettingsProvider — Read-only interface supplying application settings flow.
 */
interface SettingsProvider {
    val settingsFlow: Flow<AppSettings>
}

/**
 * SettingsRepository — Manages read and write operations for application configuration.
 *
 * Responsibility: Persists user preferences using Jetpack DataStore.
 * Depends on: [Context], [DataStore], [AppSettings].
 * Notes: Never persists clipboard contents (§9). Only non-sensitive app settings are stored here.
 */
class SettingsRepository(
    private val context: Context
) : SettingsProvider {
    private object PreferencesKeys {
        val TYPING_DELAY_MS = longPreferencesKey("typing_delay_ms")
        val SKIP_UNSUPPORTED = booleanPreferencesKey("skip_unsupported")
        val REPLACEMENT_CHAR = stringPreferencesKey("replacement_char")
        val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val AUTO_CLEAR_CLIPBOARD = booleanPreferencesKey("auto_clear_clipboard")
        val ACTIVE_DEVICE_ADDRESS = stringPreferencesKey("active_device_address")
    }

    /**
     * Observable stream of current [AppSettings].
     */
    override val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Timber.e(exception, "Error reading DataStore preferences")
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                typingDelayMs = preferences[PreferencesKeys.TYPING_DELAY_MS] ?: 2L,
                skipUnsupportedChars = preferences[PreferencesKeys.SKIP_UNSUPPORTED] ?: true,
                unsupportedCharReplacement = preferences[PreferencesKeys.REPLACEMENT_CHAR] ?: "?",
                showPersistentNotification = preferences[PreferencesKeys.SHOW_NOTIFICATION] ?: true,
                autoClearClipboard = preferences[PreferencesKeys.AUTO_CLEAR_CLIPBOARD] ?: false,
                activeDeviceAddress = preferences[PreferencesKeys.ACTIVE_DEVICE_ADDRESS]
            )
        }

    /**
     * Updates the typing delay between simulated keystrokes in milliseconds.
     *
     * @param delayMs Delay in milliseconds (typically 5ms to 50ms).
     */
    suspend fun updateTypingDelay(delayMs: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TYPING_DELAY_MS] = delayMs
        }
    }

    /**
     * Configures whether unsupported characters are skipped or replaced.
     *
     * @param skip True to drop unsupported characters, false to replace them.
     */
    suspend fun updateSkipUnsupported(skip: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SKIP_UNSUPPORTED] = skip
        }
    }

    /**
     * Configures the fallback character string used when unsupported characters are replaced.
     *
     * @param replacement Single character replacement string (e.g., "?").
     */
    suspend fun updateReplacementChar(replacement: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLACEMENT_CHAR] = replacement
        }
    }

    /**
     * Configures whether the persistent connection notification should be displayed.
     *
     * @param show True to display the foreground service notification.
     */
    suspend fun updateShowNotification(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_NOTIFICATION] = show
        }
    }

    /**
     * Configures whether the device clipboard is cleared after transmission.
     *
     * @param autoClear True to clear system clipboard after sending.
     */
    suspend fun updateAutoClearClipboard(autoClear: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CLEAR_CLIPBOARD] = autoClear
        }
    }

    /**
     * Remembers the MAC address of the preferred Bluetooth host PC.
     *
     * @param address Bluetooth hardware MAC address.
     */
    suspend fun updateActiveDeviceAddress(address: String?) {
        context.dataStore.edit { preferences ->
            if (address != null) {
                preferences[PreferencesKeys.ACTIVE_DEVICE_ADDRESS] = address
            } else {
                preferences.remove(PreferencesKeys.ACTIVE_DEVICE_ADDRESS)
            }
        }
    }
}
