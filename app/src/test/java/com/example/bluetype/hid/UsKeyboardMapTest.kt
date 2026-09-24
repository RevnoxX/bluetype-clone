package com.example.bluetype.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UsKeyboardMapTest — Unit tests for US keyboard character-to-HID-report mapping.
 *
 * Responsibility: Verifies the integrity of character mappings, shift states, and boundary conditions.
 * Depends on: [UsKeyboardMap], [KeyReport].
 * Notes: Pure JVM test, runs instantly without Android framework.
 */
class UsKeyboardMapTest {

    @Test
    fun testLowercaseLetters() {
        for (i in 0 until 26) {
            val char = ('a'.code + i).toChar()
            val report = UsKeyboardMap.getReport(char)
            assertNotNull("Letter $char should be mapped", report)
            assertEquals("Letter $char should have no modifier", KeyReport.MODIFIER_NONE, report!!.modifier)
            assertEquals("Letter $char should have keycode", (0x04 + i).toByte(), report.keycode)
        }
    }

    @Test
    fun testUppercaseLetters() {
        for (i in 0 until 26) {
            val char = ('A'.code + i).toChar()
            val report = UsKeyboardMap.getReport(char)
            assertNotNull("Letter $char should be mapped", report)
            assertEquals("Letter $char must have Shift modifier", KeyReport.MODIFIER_LEFT_SHIFT, report!!.modifier)
            assertEquals("Letter $char keycode should match lowercase keycode", (0x04 + i).toByte(), report.keycode)
        }
    }

    @Test
    fun testDigits() {
        for (i in 1..9) {
            val char = ('0'.code + i).toChar()
            val report = UsKeyboardMap.getReport(char)
            assertNotNull("Digit $char should be mapped", report)
            assertEquals("Digit $char should have no modifier", KeyReport.MODIFIER_NONE, report!!.modifier)
            assertEquals("Digit $char keycode", (0x1E + (i - 1)).toByte(), report.keycode)
        }
        val zeroReport = UsKeyboardMap.getReport('0')
        assertNotNull(zeroReport)
        assertEquals(0x27.toByte(), zeroReport!!.keycode)
    }

    @Test
    fun testCommonPunctuationAndSymbols() {
        val shiftSymbols = mapOf(
            '!' to 0x1E.toByte(),
            '@' to 0x1F.toByte(),
            '#' to 0x20.toByte(),
            '$' to 0x21.toByte(),
            '%' to 0x22.toByte(),
            '^' to 0x23.toByte(),
            '&' to 0x24.toByte(),
            '*' to 0x25.toByte(),
            '(' to 0x26.toByte(),
            ')' to 0x27.toByte(),
            '_' to 0x2D.toByte(),
            '+' to 0x2E.toByte(),
            '{' to 0x2F.toByte(),
            '}' to 0x30.toByte(),
            '|' to 0x31.toByte(),
            ':' to 0x33.toByte(),
            '"' to 0x34.toByte(),
            '~' to 0x35.toByte(),
            '<' to 0x36.toByte(),
            '>' to 0x37.toByte(),
            '?' to 0x38.toByte()
        )

        for ((char, expectedCode) in shiftSymbols) {
            val report = UsKeyboardMap.getReport(char)
            assertNotNull("Symbol $char should be mapped", report)
            assertEquals("Symbol $char must have Shift modifier", KeyReport.MODIFIER_LEFT_SHIFT, report!!.modifier)
            assertEquals("Symbol $char keycode", expectedCode, report.keycode)
        }

        val unshiftSymbols = mapOf(
            ' ' to 0x2C.toByte(),
            '\n' to 0x28.toByte(),
            '\t' to 0x2B.toByte(),
            '-' to 0x2D.toByte(),
            '=' to 0x2E.toByte(),
            '[' to 0x2F.toByte(),
            ']' to 0x30.toByte(),
            '\\' to 0x31.toByte(),
            ';' to 0x33.toByte(),
            '\'' to 0x34.toByte(),
            '`' to 0x35.toByte(),
            ',' to 0x36.toByte(),
            '.' to 0x37.toByte(),
            '/' to 0x38.toByte()
        )

        for ((char, expectedCode) in unshiftSymbols) {
            val report = UsKeyboardMap.getReport(char)
            assertNotNull("Symbol $char should be mapped", report)
            assertEquals("Symbol $char should have no modifier", KeyReport.MODIFIER_NONE, report!!.modifier)
            assertEquals("Symbol $char keycode", expectedCode, report.keycode)
        }
    }

    @Test
    fun testUnsupportedCharactersReturnNull() {
        val unsupported = listOf('ä', 'ñ', '中', '€', '₹', '§', '©')
        for (char in unsupported) {
            assertNull("Character '$char' should not be supported in US HID table", UsKeyboardMap.getReport(char))
            assertFalse(UsKeyboardMap.isSupported(char))
        }
    }

    @Test
    fun testIsSupportedReturnsTrueForValidChars() {
        assertTrue(UsKeyboardMap.isSupported('a'))
        assertTrue(UsKeyboardMap.isSupported('Z'))
        assertTrue(UsKeyboardMap.isSupported('5'))
        assertTrue(UsKeyboardMap.isSupported('\n'))
        assertTrue(UsKeyboardMap.isSupported(' '))
    }
}
