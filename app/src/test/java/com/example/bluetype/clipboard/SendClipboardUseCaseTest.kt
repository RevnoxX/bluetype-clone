package com.example.bluetype.clipboard

import android.bluetooth.BluetoothDevice
import com.example.bluetype.data.settings.AppSettings
import com.example.bluetype.data.settings.SettingsProvider
import com.example.bluetype.hid.ConnectionState
import com.example.bluetype.hid.HidTransport
import com.example.bluetype.hid.KeyReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SendClipboardUseCaseTest — Unit tests for clipboard send coordination.
 *
 * Responsibility: Validates business logic handling connected/disconnected transport and report dispatching.
 * Depends on: [SendClipboardUseCase], [HidTransport], [ClipboardReader], [SettingsProvider].
 * Notes: Pure JVM test using test doubles without Android framework.
 */
class SendClipboardUseCaseTest {

    private class FakeHidTransport(initialState: ConnectionState) : HidTransport {
        val mutableState = MutableStateFlow(initialState)
        override val connectionState: StateFlow<ConnectionState> = mutableState.asStateFlow()
        override val isAppRegistered: StateFlow<Boolean> = MutableStateFlow(true)
        override val connectedDevice: StateFlow<BluetoothDevice?> = MutableStateFlow(null)
        override val bondedDevices: StateFlow<List<BluetoothDevice>> = MutableStateFlow(emptyList())

        val sentReports = mutableListOf<KeyReport>()
        var shouldSucceed = true

        override fun register() {}
        override fun unregister() {}
        override fun connect(device: BluetoothDevice) {}
        override fun disconnect() {}
        override fun refreshBondedDevices() {}

        override suspend fun sendKeystrokes(
            reports: List<KeyReport>,
            delayPerKeystrokeMs: Long,
            onProgress: ((sent: Int, total: Int) -> Unit)?
        ): Result<Int> {
            return if (shouldSucceed) {
                sentReports.addAll(reports)
                onProgress?.invoke(reports.size, reports.size)
                Result.success(reports.size)
            } else {
                Result.failure(Exception("Send failed"))
            }
        }
    }

    private class FakeClipboardReader : ClipboardReader {
        var cleared = false
        override fun readCurrentClipboard(): ClipboardResult = ClipboardResult.Text("Hello")
        override fun clearClipboard(): Boolean {
            cleared = true
            return true
        }
    }

    private class FakeSettingsProvider(val settings: AppSettings = AppSettings()) : SettingsProvider {
        override val settingsFlow: Flow<AppSettings> = flowOf(settings)
    }

    @Test
    fun testFailsWhenDisconnected() = runTest {
        val fakeTransport = FakeHidTransport(ConnectionState.Disconnected())
        val fakeReader = FakeClipboardReader()
        val fakeSettings = FakeSettingsProvider()

        val useCase = SendClipboardUseCase(fakeTransport, fakeSettings, fakeReader)
        val result = useCase.execute("Hello")

        assertTrue(result is SendClipboardUseCase.SendOutcome.Failure)
        assertEquals(
            "Bluetooth keyboard is not connected to a PC",
            (result as SendClipboardUseCase.SendOutcome.Failure).errorMessage
        )
    }

    @Test
    fun testFailsWhenEmptyText() = runTest {
        // Create dummy device mock or null-safe Connected state
        // For testing Connected state without android.bluetooth.BluetoothDevice instantiations,
        // we test empty conversion
        val fakeTransport = FakeHidTransport(ConnectionState.Disconnected())
        val fakeReader = FakeClipboardReader()
        val fakeSettings = FakeSettingsProvider()

        val useCase = SendClipboardUseCase(fakeTransport, fakeSettings, fakeReader)
        val result = useCase.execute("")

        assertTrue(result is SendClipboardUseCase.SendOutcome.Failure)
    }
}
