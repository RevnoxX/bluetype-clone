package com.example.bluetype.viewmodel

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetype.clipboard.ClipboardReader
import com.example.bluetype.clipboard.ClipboardResult
import com.example.bluetype.clipboard.SendClipboardUseCase
import com.example.bluetype.data.settings.AppSettings
import com.example.bluetype.data.settings.SettingsRepository
import com.example.bluetype.hid.BluetoothScanner
import com.example.bluetype.hid.ConnectionState
import com.example.bluetype.hid.DiscoveredBluetoothDevice
import com.example.bluetype.hid.HidTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * MainUiState — Encapsulates the UI state for the main BlueType screen.
 */
data class MainUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected(),
    val isAppRegistered: Boolean = false,
    val connectedDevice: BluetoothDevice? = null,
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val nearbyDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val isSending: Boolean = false,
    val sendProgress: Pair<Int, Int>? = null,
    val pendingSensitiveText: String? = null,
    val feedbackMessage: String? = null
)

/**
 * MainViewModel — Coordinates HID transport, bluetooth scanning, settings, and clipboard transmission.
 *
 * Responsibility: Holds UI state and handles user actions for device connection and clipboard typing.
 * Depends on: [HidTransport], [BluetoothScanner], [ClipboardReader], [SendClipboardUseCase], [SettingsRepository].
 * Notes: Ensures that sensitive clipboard contents require user confirmation (§9).
 */
class MainViewModel(
    private val hidTransport: HidTransport,
    private val bluetoothScanner: BluetoothScanner,
    private val clipboardReader: ClipboardReader,
    private val sendClipboardUseCase: SendClipboardUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // Collect HID connection state
        viewModelScope.launch {
            hidTransport.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    connectedDevice = (state as? ConnectionState.Connected)?.device
                )
            }
        }

        // Collect registered state
        viewModelScope.launch {
            hidTransport.isAppRegistered.collect { registered ->
                _uiState.value = _uiState.value.copy(isAppRegistered = registered)
            }
        }

        // Collect bonded devices
        viewModelScope.launch {
            hidTransport.bondedDevices.collect { bonded ->
                _uiState.value = _uiState.value.copy(bondedDevices = bonded)
            }
        }

        // Collect discovered nearby devices
        viewModelScope.launch {
            bluetoothScanner.discoveredDevices.collect { discovered ->
                _uiState.value = _uiState.value.copy(nearbyDevices = discovered)
            }
        }

        // Collect scanning status
        viewModelScope.launch {
            bluetoothScanner.isScanning.collect { scanning ->
                _uiState.value = _uiState.value.copy(isScanning = scanning)
            }
        }

        // Initialize registration
        hidTransport.register()
    }

    /**
     * Reads the current clipboard from the focused activity context and sends to connected PC.
     */
    fun onSendClipboardClicked() {
        val currentState = _uiState.value.connectionState
        if (currentState !is ConnectionState.Connected) {
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "[ERR_BT_NOT_CONNECTED: 0x01] Connect to a Windows PC before sending clipboard"
            )
            return
        }

        when (val clipResult = clipboardReader.readCurrentClipboard()) {
            is ClipboardResult.Empty -> {
                _uiState.value = _uiState.value.copy(feedbackMessage = "[ERR_CLIPBOARD_EMPTY: 0x20] Clipboard is empty")
            }
            is ClipboardResult.Unreadable -> {
                _uiState.value = _uiState.value.copy(feedbackMessage = "[ERR_CLIPBOARD_UNREADABLE: 0x21] ${clipResult.reason}")
            }
            is ClipboardResult.Sensitive -> {
                // Prompt user for confirmation before sending sensitive text (§9)
                _uiState.value = _uiState.value.copy(pendingSensitiveText = clipResult.text)
            }
            is ClipboardResult.Text -> {
                transmitText(clipResult.text)
            }
        }
    }

    /**
     * Confirms and sends sensitive content previously held in [MainUiState.pendingSensitiveText].
     */
    fun confirmSensitiveSend() {
        val text = _uiState.value.pendingSensitiveText ?: return
        _uiState.value = _uiState.value.copy(pendingSensitiveText = null)
        transmitText(text)
    }

    /**
     * Dismisses the sensitive clipboard prompt without sending.
     */
    fun dismissSensitivePrompt() {
        _uiState.value = _uiState.value.copy(pendingSensitiveText = null)
    }

    private fun transmitText(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSending = true,
                sendProgress = Pair(0, text.length)
            )

            var lastProgressUpdate = 0L
            val outcome = sendClipboardUseCase.execute(text) { sent, total ->
                val now = System.currentTimeMillis()
                if (sent == total || now - lastProgressUpdate >= 80L) {
                    lastProgressUpdate = now
                    _uiState.value = _uiState.value.copy(sendProgress = Pair(sent, total))
                }
            }

            when (outcome) {
                is SendClipboardUseCase.SendOutcome.Success -> {
                    val msg = if (outcome.droppedCount > 0) {
                        "Sent ${outcome.charactersSent} characters (${outcome.droppedCount} unsupported skipped)"
                    } else {
                        "Sent ${outcome.charactersSent} characters to PC"
                    }
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        sendProgress = null,
                        feedbackMessage = msg
                    )
                }
                is SendClipboardUseCase.SendOutcome.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        sendProgress = null,
                        feedbackMessage = outcome.errorMessage
                    )
                }
            }
        }
    }

    /**
     * Clears transient feedback message from the UI.
     */
    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    /**
     * Connects to the selected Bluetooth host PC.
     */
    fun connectDevice(device: BluetoothDevice) {
        hidTransport.connect(device)
        viewModelScope.launch {
            settingsRepository.updateActiveDeviceAddress(device.address)
        }
    }

    /**
     * Disconnects the currently active Bluetooth host connection.
     */
    fun disconnectDevice() {
        hidTransport.disconnect()
    }

    /**
     * Starts scanning for nearby Bluetooth devices.
     */
    fun startScan() {
        bluetoothScanner.startScan()
    }

    /**
     * Stops active device scanning.
     */
    fun stopScan() {
        bluetoothScanner.stopScan()
    }

    /**
     * Refreshes list of bonded devices from Bluetooth adapter.
     */
    fun refreshBondedDevices() {
        hidTransport.refreshBondedDevices()
    }

    // Settings actions
    fun setTypingDelay(delayMs: Long) {
        viewModelScope.launch { settingsRepository.updateTypingDelay(delayMs) }
    }

    fun setSkipUnsupported(skip: Boolean) {
        viewModelScope.launch { settingsRepository.updateSkipUnsupported(skip) }
    }

    fun setReplacementChar(char: String) {
        viewModelScope.launch { settingsRepository.updateReplacementChar(char) }
    }

    fun setShowNotification(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowNotification(show) }
    }

    fun setAutoClear(autoClear: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoClearClipboard(autoClear) }
    }

    override fun onCleared() {
        bluetoothScanner.cleanup()
        super.onCleared()
    }

    /**
     * Factory for creating MainViewModel with manual DI from AppContainer.
     */
    class Factory(
        private val hidTransport: HidTransport,
        private val bluetoothScanner: BluetoothScanner,
        private val clipboardReader: ClipboardReader,
        private val sendClipboardUseCase: SendClipboardUseCase,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                hidTransport,
                bluetoothScanner,
                clipboardReader,
                sendClipboardUseCase,
                settingsRepository
            ) as T
        }
    }
}
