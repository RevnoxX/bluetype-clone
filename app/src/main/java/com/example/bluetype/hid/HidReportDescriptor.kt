package com.example.bluetype.hid

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

/**
 * HidReportDescriptor — HID report descriptor and SDP service record configuration.
 *
 * Responsibility: Defines the binary USB HID report structure for the keyboard profile.
 * Depends on: [BluetoothHidDeviceAppSdpSettings].
 * Notes: Uses standard USB HID usage page 0x07 (Keyboard) with Report ID 1.
 */
object HidReportDescriptor {

    /**
     * Report ID for the keyboard endpoint.
     */
    const val REPORT_ID_KEYBOARD: Byte = 1

    /**
     * Standard USB HID Keyboard Report Descriptor with Report ID 1.
     * Report format:
     * - Byte 0: Modifiers (Left Ctrl, Shift, Alt, GUI, Right Ctrl, Shift, Alt, GUI)
     * - Byte 1: Reserved (0x00)
     * - Bytes 2..7: Key codes (up to 6 concurrent pressed keys)
     */
    val KEYBOARD_DESCRIPTOR = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),       // USAGE_PAGE (Generic Desktop)
        0x09.toByte(), 0x06.toByte(),       // USAGE (Keyboard)
        0xA1.toByte(), 0x01.toByte(),       // COLLECTION (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD,  //   REPORT_ID (1)
        0x05.toByte(), 0x07.toByte(),       //   USAGE_PAGE (Keyboard)
        0x19.toByte(), 0xE0.toByte(),       //   USAGE_MINIMUM (Keyboard LeftControl)
        0x29.toByte(), 0xE7.toByte(),       //   USAGE_MAXIMUM (Keyboard Right GUI)
        0x15.toByte(), 0x00.toByte(),       //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x01.toByte(),       //   LOGICAL_MAXIMUM (1)
        0x75.toByte(), 0x01.toByte(),       //   REPORT_SIZE (1)
        0x95.toByte(), 0x08.toByte(),       //   REPORT_COUNT (8)
        0x81.toByte(), 0x02.toByte(),       //   INPUT (Data,Var,Abs) - Modifier Byte
        0x95.toByte(), 0x01.toByte(),       //   REPORT_COUNT (1)
        0x75.toByte(), 0x08.toByte(),       //   REPORT_SIZE (8)
        0x81.toByte(), 0x03.toByte(),       //   INPUT (Cnst,Var,Abs) - Reserved Byte
        0x95.toByte(), 0x05.toByte(),       //   REPORT_COUNT (5)
        0x75.toByte(), 0x01.toByte(),       //   REPORT_SIZE (1)
        0x05.toByte(), 0x08.toByte(),       //   USAGE_PAGE (LEDs)
        0x19.toByte(), 0x01.toByte(),       //   USAGE_MINIMUM (Num Lock)
        0x29.toByte(), 0x05.toByte(),       //   USAGE_MAXIMUM (Kana)
        0x91.toByte(), 0x02.toByte(),       //   OUTPUT (Data,Var,Abs) - LED report
        0x95.toByte(), 0x01.toByte(),       //   REPORT_COUNT (1)
        0x75.toByte(), 0x03.toByte(),       //   REPORT_SIZE (3)
        0x91.toByte(), 0x03.toByte(),       //   OUTPUT (Cnst,Var,Abs) - LED padding
        0x95.toByte(), 0x06.toByte(),       //   REPORT_COUNT (6)
        0x75.toByte(), 0x08.toByte(),       //   REPORT_SIZE (8)
        0x15.toByte(), 0x00.toByte(),       //   LOGICAL_MINIMUM (0)
        0x25.toByte(), 0x65.toByte(),       //   LOGICAL_MAXIMUM (101)
        0x05.toByte(), 0x07.toByte(),       //   USAGE_PAGE (Keyboard)
        0x19.toByte(), 0x00.toByte(),       //   USAGE_MINIMUM (Reserved)
        0x29.toByte(), 0x65.toByte(),       //   USAGE_MAXIMUM (Keyboard Application)
        0x81.toByte(), 0x00.toByte(),       //   INPUT (Data,Ary,Abs) - Key array
        0xC0.toByte()                       // END_COLLECTION
    )

    /**
     * Builds standard SDP record settings for the Bluetooth HID keyboard profile.
     *
     * @param deviceName Display name broadcast to pairing PCs.
     * @return [BluetoothHidDeviceAppSdpSettings] ready for registration.
     */
    fun buildSdpSettings(deviceName: String = "BlueType Keyboard"): BluetoothHidDeviceAppSdpSettings {
        return BluetoothHidDeviceAppSdpSettings(
            deviceName,
            "Bluetooth HID Clipboard Bridge",
            "BlueType",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            KEYBOARD_DESCRIPTOR
        )
    }
}
