package com.example.bluetype.di

import android.content.Context
import com.example.bluetype.hid.BluetoothClassicHidTransport
import com.example.bluetype.hid.HidTransport

/**
 * TransportModule — Provider module for HidTransport implementation.
 *
 * Responsibility: Resolves HidTransport to BluetoothClassicHidTransport.
 * Depends on: [Context], [HidTransport], [BluetoothClassicHidTransport].
 * Notes: Ensures transport implementation can be swapped for BLE HOGP if needed (§6).
 */
object TransportModule {
    fun provideHidTransport(context: Context): HidTransport {
        return BluetoothClassicHidTransport(context)
    }
}
