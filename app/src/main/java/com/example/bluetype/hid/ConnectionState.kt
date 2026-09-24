package com.example.bluetype.hid

import android.bluetooth.BluetoothDevice

/**
 * ConnectionState — Models the lifecycle state of the Bluetooth HID device connection.
 *
 * Responsibility: Represents the sealed hierarchy of HID connection states.
 * Depends on: [BluetoothDevice].
 * Notes: Distinguishes between unsupported hardware vs normal disconnect per §4.1.
 */
sealed class ConnectionState {

    /**
     * No active HID connection to any host PC.
     *
     * @property reason Optional diagnostic reason for disconnection.
     */
    data class Disconnected(val reason: String? = null) : ConnectionState()

    /**
     * Actively negotiating HID connection or SDP handshake with a host PC.
     *
     * @property device Target host device if known.
     * @property deviceName Human-readable device display name.
     */
    data class Connecting(
        val device: BluetoothDevice? = null,
        val deviceName: String = "Connecting..."
    ) : ConnectionState()

    /**
     * Successfully connected and paired as a Bluetooth HID keyboard.
     *
     * @property device Connected host PC BluetoothDevice.
     * @property deviceName Resolved human-readable name of the host PC.
     */
    data class Connected(
        val device: BluetoothDevice,
        val deviceName: String
    ) : ConnectionState()

    /**
     * The phone's Bluetooth chipset or firmware does not support the HID device role.
     *
     * @property reason Explanatory failure reason from the Bluetooth stack (§4.1).
     */
    data class Unsupported(
        val reason: String = "Your phone's Bluetooth hardware does not support acting as a keyboard"
    ) : ConnectionState()

    /**
     * Convenience property indicating whether text can currently be typed over HID.
     */
    val isConnected: Boolean
        get() = this is Connected

    /**
     * Convenience property indicating whether a connection attempt is in flight.
     */
    val isConnecting: Boolean
        get() = this is Connecting
}
