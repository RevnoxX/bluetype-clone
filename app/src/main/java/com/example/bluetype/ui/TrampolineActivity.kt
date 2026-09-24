package com.example.bluetype.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.bluetype.BlueTypeApplication
import com.example.bluetype.R
import com.example.bluetype.clipboard.ClipboardResult
import com.example.bluetype.clipboard.SendClipboardUseCase
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * TrampolineActivity — Translucent no-UI activity used to briefly acquire focus to read the clipboard.
 *
 * Responsibility: Obtains window focus, reads system clipboard via FocusedClipboardReader, and triggers transmission.
 * Depends on: [FocusedClipboardReader], [SendClipboardUseCase], [BlueTypeApplication].
 * Notes: Exists solely to satisfy Android 10+ clipboard input focus restrictions (§4.2). Finishes immediately.
 */
class TrampolineActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No layout inflated — transparent window
    }

    override fun onResume() {
        super.onResume()
        // Focus is acquired in onResume; read clipboard immediately
        processClipboardSend()
    }

    private fun processClipboardSend() {
        val app = application as? BlueTypeApplication
        if (app == null) {
            finish()
            return
        }

        val clipboardReader = app.container.clipboardReader
        val sendUseCase = app.container.sendClipboardUseCase

        when (val result = clipboardReader.readCurrentClipboard()) {
            is ClipboardResult.Empty -> {
                showToast(getString(R.string.clipboard_empty))
                finish()
            }
            is ClipboardResult.Unreadable -> {
                showToast(result.reason)
                finish()
            }
            is ClipboardResult.Sensitive -> {
                // Sensitive clipboard item (§9) requires explicit user confirmation
                AlertDialog.Builder(this)
                    .setTitle(R.string.sensitive_clip_title)
                    .setMessage(R.string.sensitive_clip_message)
                    .setPositiveButton(R.string.action_send) { _, _ ->
                        performSend(result.text, sendUseCase)
                    }
                    .setNegativeButton(R.string.action_cancel) { _, _ ->
                        finish()
                    }
                    .setOnCancelListener { finish() }
                    .show()
            }
            is ClipboardResult.Text -> {
                performSend(result.text, sendUseCase)
            }
        }
    }

    private fun performSend(text: String, sendUseCase: SendClipboardUseCase) {
        lifecycleScope.launch {
            val outcome = sendUseCase.execute(text)
            when (outcome) {
                is SendClipboardUseCase.SendOutcome.Success -> {
                    triggerHapticFeedback()
                    if (outcome.droppedCount > 0) {
                        showToast(
                            getString(
                                R.string.send_success_with_dropped,
                                outcome.charactersSent,
                                outcome.droppedCount
                            )
                        )
                    } else {
                        showToast(getString(R.string.send_success, outcome.charactersSent))
                    }
                }
                is SendClipboardUseCase.SendOutcome.Failure -> {
                    showToast(outcome.errorMessage)
                }
            }
            finish()
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(40)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Haptic feedback failed")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
