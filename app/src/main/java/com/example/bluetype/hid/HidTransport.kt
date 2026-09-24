package com.example.bluetype.hid

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * HidTransport — Swappable interface abstraction over Bluetooth HID transports.
 *
 * Responsibility: Defines the contract for registering, connecting, and sending HID keystrokes.
 * Depends on: [ConnectionState], [KeyReport], [BluetoothDevice].
 * Notes: Keeps transport swappable between Classic HID and future BLE HOGP (§5 & §6).
 */
interface HidTransport {

    /** Current connection state observable by UI and background services. */
    val connectionState: StateFlow<ConnectionState>

    /** Whether the HID app profile is registered with the system Bluetooth stack. */
    val isAppRegistered: StateFlow<Boolean>

    /** The currently connected host device, or null if disconnected. */
    val connectedDevice: StateFlow<BluetoothDevice?>

    /** List of bonded Bluetooth devices available for connection. */
    val bondedDevices: StateFlow<List<BluetoothDevice>>

    /**
     * Initializes the Bluetooth profile proxy and registers the HID service descriptor.
     */
    fun register()

    /**
     * Unregisters the HID service descriptor and cleans up Bluetooth profile proxies.
     */
    fun unregister()

    /**
     * Initiates connection to a target Bluetooth host PC.
     *
     * @param device The bonded or discovered Bluetooth device to connect to.
     */
    fun connect(device: BluetoothDevice)

    /**
     * Disconnects the active HID connection if currently connected.
     */
    fun disconnect()

    /**
     * Refreshes the list of bonded devices from the Bluetooth adapter.
     */
    fun refreshBondedDevices()

    /**
     * Transmits a sequence of HID key reports with specified inter-key delay.
     *
     * @param reports Sequence of alternating down/empty [KeyReport]s.
     * @param delayPerKeystrokeMs Delay between keystrokes to prevent OS buffer overrun.
     * @param onProgress Optional callback reporting (sentKeystrokes, totalKeystrokes).
     * @return [Result] containing the total count of successfully sent key reports.
     */
    suspend fun sendKeystrokes(
        reports: List<KeyReport>,
        delayPerKeystrokeMs: Long,
        onProgress: ((sent: Int, total: Int) -> Unit)? = null
    ): Result<Int>
}
