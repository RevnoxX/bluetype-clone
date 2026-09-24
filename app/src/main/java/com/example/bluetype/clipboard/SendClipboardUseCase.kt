package com.example.bluetype.clipboard

import com.example.bluetype.data.settings.SettingsProvider
import com.example.bluetype.hid.ConnectionState
import com.example.bluetype.hid.HidTransport
import com.example.bluetype.hid.TextToKeystrokes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * SendClipboardUseCase — Coordinates reading, converting, and transmitting clipboard text over HID.
 *
 * Responsibility: Business logic orchestrator for translating text to keystrokes and dispatching to HidTransport.
 * Depends on: [HidTransport], [SettingsProvider], [ClipboardReader], [TextToKeystrokes].
 * Notes: Never logs raw clipboard payload per §9 security guidelines.
 */
class SendClipboardUseCase(
    private val hidTransport: HidTransport,
    private val settingsProvider: SettingsProvider,
    private val clipboardReader: ClipboardReader
) {
    /**
     * Outcome of a clipboard sending operation.
     */
    sealed class SendOutcome {
        data class Success(val charactersSent: Int, val droppedCount: Int) : SendOutcome()
        data class Failure(val errorMessage: String) : SendOutcome()
    }

    /**
     * Transmits the given text to the active Bluetooth HID host PC.
     *
     * @param text Text content to type.
     * @param onProgress Optional progress callback (keystrokesSent, totalKeystrokes).
     * @return [SendOutcome] with success or failure details.
     */
    suspend fun execute(
        text: String,
        onProgress: ((sent: Int, total: Int) -> Unit)? = null
    ): SendOutcome = withContext(Dispatchers.IO) {
        val state = hidTransport.connectionState.value
        if (state !is ConnectionState.Connected) {
            return@withContext SendOutcome.Failure("[ERR_BT_NOT_CONNECTED: 0x01] Bluetooth keyboard is not connected to a PC")
        }

        val settings = settingsProvider.settingsFlow.first()
        val replacementChar = settings.unsupportedCharReplacement.firstOrNull() ?: '?'

        val conversion = TextToKeystrokes.convert(
            text = text,
            skipUnsupported = settings.skipUnsupportedChars,
            replacementChar = replacementChar
        )

        if (conversion.reports.isEmpty()) {
            if (text.isNotEmpty()) {
                SendOutcome.Failure(
                    "[ERR_CHARS_UNSUPPORTED: 0x30] All ${text.length} characters were unsupported by the keyboard table"
                )
            } else {
                SendOutcome.Failure("[ERR_CLIPBOARD_EMPTY: 0x20] Nothing to send")
            }
        } else {
            Timber.d("Starting transmission: %d reports to send", conversion.reports.size)

            val result = hidTransport.sendKeystrokes(
                reports = conversion.reports,
                delayPerKeystrokeMs = settings.typingDelayMs,
                onProgress = onProgress
            )

            if (result.isSuccess) {
                if (settings.autoClearClipboard) {
                    clipboardReader.clearClipboard()
                }
                SendOutcome.Success(
                    charactersSent = conversion.reports.size / 2,
                    droppedCount = conversion.droppedCharCount
                )
            } else {
                val error = result.exceptionOrNull()?.message ?: "[ERR_SEND_FAILED: 0x05] Keystroke transmission failed"
                SendOutcome.Failure(error)
            }
        }
    }
}
