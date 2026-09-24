package com.example.bluetype.hid

/**
 * UsKeyboardMap — Lookup table mapping Unicode characters to US QWERTY HID keystrokes.
 *
 * Responsibility: Maps printable ASCII and standard control characters to HID usage codes and shift state.
 * Depends on: [KeyReport].
 * Notes: Assumes US QWERTY keyboard layout on the host OS. Layout differences may affect special characters.
 */
object UsKeyboardMap {

    private val map: Map<Char, KeyReport> = buildMap {
        // Lowercase letters 'a'..'z' (HID usage 0x04 to 0x1D)
        for (i in 0 until 26) {
            val char = ('a'.code + i).toChar()
            val code = (0x04 + i).toByte()
            put(char, KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = code))
        }

        // Uppercase letters 'A'..'Z' (HID usage 0x04 to 0x1D with Left Shift)
        for (i in 0 until 26) {
            val char = ('A'.code + i).toChar()
            val code = (0x04 + i).toByte()
            put(char, KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = code))
        }

        // Digits '1'..'9' (0x1E to 0x26) and '0' (0x27)
        for (i in 1..9) {
            val char = ('0'.code + i).toChar()
            val code = (0x1E + (i - 1)).toByte()
            put(char, KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = code))
        }
        put('0', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x27.toByte()))

        // Shifted digit symbols
        put('!', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x1E.toByte()))
        put('@', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x1F.toByte()))
        put('#', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x20.toByte()))
        put('$', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x21.toByte()))
        put('%', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x22.toByte()))
        put('^', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x23.toByte()))
        put('&', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x24.toByte()))
        put('*', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x25.toByte()))
        put('(', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x26.toByte()))
        put(')', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x27.toByte()))

        // Whitespace and Control
        put('\n', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x28.toByte())) // Enter
        put('\t', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x2B.toByte())) // Tab
        put(' ', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x2C.toByte()))  // Space

        // Punctuation and symbols
        put('-', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x2D.toByte()))
        put('_', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x2D.toByte()))
        put('=', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x2E.toByte()))
        put('+', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x2E.toByte()))
        put('[', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x2F.toByte()))
        put('{', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x2F.toByte()))
        put(']', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x30.toByte()))
        put('}', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x30.toByte()))
        put('\\', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x31.toByte()))
        put('|', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x31.toByte()))
        put(';', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x33.toByte()))
        put(':', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x33.toByte()))
        put('\'', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x34.toByte()))
        put('"', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x34.toByte()))
        put('`', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x35.toByte()))
        put('~', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x35.toByte()))
        put(',', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x36.toByte()))
        put('<', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x36.toByte()))
        put('.', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x37.toByte()))
        put('>', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x37.toByte()))
        put('/', KeyReport(modifier = KeyReport.MODIFIER_NONE, keycode = 0x38.toByte()))
        put('?', KeyReport(modifier = KeyReport.MODIFIER_LEFT_SHIFT, keycode = 0x38.toByte()))
    }

    /**
     * Looks up the corresponding [KeyReport] for a character.
     *
     * @param char The character to look up.
     * @return [KeyReport] if character is supported on US keyboard, or null if unsupported.
     */
    fun getReport(char: Char): KeyReport? = map[char]

    /**
     * Checks if a character is supported by the US keyboard map.
     *
     * @param char Character to check.
     * @return True if supported, false otherwise.
     */
    fun isSupported(char: Char): Boolean = map.containsKey(char)
}
