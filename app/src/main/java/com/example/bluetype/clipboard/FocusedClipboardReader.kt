package com.example.bluetype.clipboard

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import timber.log.Timber

/**
 * FocusedClipboardReader — Concrete reader accessing ClipboardManager in focused window contexts.
 *
 * Responsibility: Interacts with Android ClipboardManager to extract text while respecting Android 10+ focus restrictions.
 * Depends on: [Context], [ClipboardManager], [ClipboardResult].
 * Notes: Must only be invoked from a focused Activity context (MainActivity or TrampolineActivity) — see §4.2.
 */
class FocusedClipboardReader(
    private val context: Context
) : ClipboardReader {

    private val clipboardManager: ClipboardManager? =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    /**
     * Reads current clipboard contents with MIME-type validation and sensitivity detection.
     *
     * @return [ClipboardResult] representing the clipboard state.
     */
    override fun readCurrentClipboard(): ClipboardResult {
        val manager = clipboardManager ?: return ClipboardResult.Unreadable("Clipboard service unavailable")

        if (!manager.hasPrimaryClip()) {
            return ClipboardResult.Empty
        }

        val primaryClip = try {
            manager.primaryClip
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException reading primary clip — window may lack input focus")
            return ClipboardResult.Unreadable("Window not focused")
        }

        if (primaryClip == null || primaryClip.itemCount == 0) {
            return ClipboardResult.Empty
        }

        val description = primaryClip.description
        val isText = description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true

        val item = primaryClip.getItemAt(0)
        val text = item?.text?.toString() ?: item?.coerceToText(context)?.toString()

        if (text.isNullOrEmpty()) {
            return if (!isText) {
                ClipboardResult.Unreadable("Only text can be sent via Bluetooth keyboard")
            } else {
                ClipboardResult.Empty
            }
        }

        // Check for Android 13+ (API 33) sensitive clip flag
        val isSensitive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
        } else {
            false
        }

        return if (isSensitive) {
            ClipboardResult.Sensitive(text)
        } else {
            ClipboardResult.Text(text)
        }
    }

    /**
     * Clears the primary clip from the clipboard manager.
     */
    override fun clearClipboard(): Boolean {
        val manager = clipboardManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                manager.clearPrimaryClip()
                true
            } else {
                manager.setPrimaryClip(ClipData.newPlainText("", ""))
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear primary clip")
            false
        }
    }
}
