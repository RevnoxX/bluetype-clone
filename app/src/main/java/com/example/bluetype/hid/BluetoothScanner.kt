package com.example.bluetype.hid

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * DiscoveredBluetoothDevice — Represents a nearby discovered Bluetooth device.
 *
 * Responsibility: Value holder for UI rendering of nearby devices.
 * Depends on: [BluetoothDevice].
 * Notes: Provides stable identifier based on MAC address for Compose LazyColumn keys.
 */
data class DiscoveredBluetoothDevice(
    val device: BluetoothDevice,
    val name: String,
    val address: String,
    val rssi: Short = 0,
    val isBonded: Boolean = false
)

/**
 * BluetoothScanner — Manages Bluetooth device discovery for nearby devices.
 *
 * Responsibility: Controls BluetoothAdapter discovery lifecycle and aggregates discovered devices.
 * Depends on: [BluetoothAdapter], [BroadcastReceiver], [DiscoveredBluetoothDevice].
 * Notes: Honors BLUETOOTH_SCAN permission flags and cleans up receivers to prevent leaks.
 */
class BluetoothScanner(
    private val context: Context
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _discoveredDevices.asStateFlow()

    private var receiverRegistered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                    if (device != null) {
                        val name = getDeviceName(device)
                        val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                        val entry = DiscoveredBluetoothDevice(
                            device = device,
                            name = name,
                            address = device.address,
                            rssi = rssi,
                            isBonded = isBonded
                        )
                        updateDevice(entry)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    private fun updateDevice(device: DiscoveredBluetoothDevice) {
        val current = _discoveredDevices.value.toMutableList()
        val index = current.indexOfFirst { it.address == device.address }
        if (index >= 0) {
            current[index] = device
        } else {
            current.add(device)
        }
        _discoveredDevices.value = current
    }

    /**
     * Starts device discovery. Clears previously discovered unbonded devices.
     */
    fun startScan() {
        if (!hasScanPermission() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return
        }

        registerReceiver()
        _discoveredDevices.value = emptyList()

        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
            _isScanning.value = true
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException while starting Bluetooth discovery")
        }
    }

    /**
     * Cancels active discovery.
     */
    fun stopScan() {
        if (!hasScanPermission() || bluetoothAdapter == null) return
        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException while canceling discovery")
        } finally {
            _isScanning.value = false
        }
    }

    private fun registerReceiver() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            context.registerReceiver(receiver, filter)
            receiverRegistered = true
        }
    }

    /**
     * Unregisters receiver and stops active scanning.
     */
    fun cleanup() {
        stopScan()
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Timber.e(e, "Error unregistering discovery receiver")
            }
            receiverRegistered = false
        }
    }

    private fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getDeviceName(device: BluetoothDevice): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.name ?: "Unknown Device"
                } else {
                    "Bluetooth Device"
                }
            } else {
                device.name ?: "Unknown Device"
            }
        } catch (e: SecurityException) {
            "Bluetooth Device"
        }
    }
}
