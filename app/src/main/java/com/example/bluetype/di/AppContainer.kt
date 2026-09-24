package com.example.bluetype.di

import android.content.Context
import com.example.bluetype.clipboard.ClipboardReader
import com.example.bluetype.clipboard.FocusedClipboardReader
import com.example.bluetype.clipboard.SendClipboardUseCase
import com.example.bluetype.data.settings.SettingsRepository
import com.example.bluetype.hid.BluetoothClassicHidTransport
import com.example.bluetype.hid.BluetoothScanner
import com.example.bluetype.hid.HidTransport

/**
 * AppContainer — Dependency injection container providing application-scoped singletons.
 *
 * Responsibility: Wires together repositories, transport layers, use cases, and bluetooth scanner.
 * Depends on: [Context], [HidTransport], [ClipboardReader], [SettingsRepository], [SendClipboardUseCase].
 * Notes: Provides clean, decoupled instantiation without reflection or code generation overhead (§5).
 */
class AppContainer(private val context: Context) {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val hidTransport: HidTransport by lazy {
        BluetoothClassicHidTransport(context)
    }

    val clipboardReader: ClipboardReader by lazy {
        FocusedClipboardReader(context)
    }

    val sendClipboardUseCase: SendClipboardUseCase by lazy {
        SendClipboardUseCase(
            hidTransport = hidTransport,
            settingsProvider = settingsRepository,
            clipboardReader = clipboardReader
        )
    }

    val bluetoothScanner: BluetoothScanner by lazy {
        BluetoothScanner(context)
    }
}
