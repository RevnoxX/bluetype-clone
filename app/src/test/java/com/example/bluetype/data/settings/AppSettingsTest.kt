package com.example.bluetype.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AppSettingsTest — Unit tests for AppSettings default configurations.
 *
 * Responsibility: Validates default values and copy immutability.
 * Depends on: [AppSettings].
 * Notes: Pure JVM test.
 */
class AppSettingsTest {

    @Test
    fun testDefaultSettings() {
        val settings = AppSettings()
        assertEquals(10L, settings.typingDelayMs)
        assertTrue(settings.skipUnsupportedChars)
        assertEquals("?", settings.unsupportedCharReplacement)
        assertTrue(settings.showPersistentNotification)
        assertFalse(settings.autoClearClipboard)
        assertNull(settings.activeDeviceAddress)
    }

    @Test
    fun testCustomSettingsCopy() {
        val original = AppSettings()
        val modified = original.copy(
            typingDelayMs = 25L,
            skipUnsupportedChars = false,
            unsupportedCharReplacement = "#",
            autoClearClipboard = true,
            activeDeviceAddress = "00:11:22:33:44:55"
        )

        assertEquals(25L, modified.typingDelayMs)
        assertFalse(modified.skipUnsupportedChars)
        assertEquals("#", modified.unsupportedCharReplacement)
        assertTrue(modified.autoClearClipboard)
        assertEquals("00:11:22:33:44:55", modified.activeDeviceAddress)
    }
}
