package com.example.bluetype.clipboard

/**
 * ClipboardReader — Contract for accessing the Android system clipboard.
 *
 * Responsibility: Defines interface for reading and optionally clearing the system clipboard.
 * Depends on: [ClipboardResult].
 * Notes: Must only be invoked from a focused window context (Android 10+ limitation §4.2).
 */
interface ClipboardReader {

    /**
     * Reads the primary clip from the system clipboard.
     *
     * @return [ClipboardResult] indicating Text, Sensitive, Empty, or Unreadable.
     */
    fun readCurrentClipboard(): ClipboardResult

    /**
     * Clears the system clipboard if configured by user settings.
     *
     * @return True if clipboard was cleared successfully.
     */
    fun clearClipboard(): Boolean
}
