package com.example.bluetype.hid

/**
 * KeyReport — Data class representing a standard USB HID 8-byte keyboard report.
 *
 * Responsibility: Encapsulates the binary byte sequence for HID keyboard transmission.
 * Depends on: None. Pure data structure.
 * Notes: Adheres to USB HID Usage Tables for standard keyboard boot protocol reports.
 */
data class KeyReport(
    val modifier: Byte = 0x00,
    val keycode: Byte = 0x00
) {
    /**
     * Converts this KeyReport to an 8-byte USB HID boot keyboard report.
     * Byte 0: Modifier bitmap (Shift, Ctrl, Alt, GUI)
     * Byte 1: Reserved (0x00)
     * Byte 2: Keycode (Usage ID)
     * Bytes 3-7: Additional simultaneous keys (0x00 for single keystroke)
     *
     * @return 8-byte array formatted for Bluetooth HID report ID 1 (keyboard).
     */
    fun toByteArray(): ByteArray {
        val bytes = ByteArray(8)
        bytes[0] = modifier
        bytes[1] = 0x00 // Reserved byte
        bytes[2] = keycode
        return bytes
    }

    companion object {
        /** Left shift modifier bitmask (0x02). */
        const val MODIFIER_NONE: Byte = 0x00
        const val MODIFIER_LEFT_CTRL: Byte = 0x01
        const val MODIFIER_LEFT_SHIFT: Byte = 0x02
        const val MODIFIER_LEFT_ALT: Byte = 0x04
        const val MODIFIER_LEFT_GUI: Byte = 0x08

        /** An empty report representing all keys released. */
        val EMPTY = KeyReport(modifier = 0x00, keycode = 0x00)
    }
}
