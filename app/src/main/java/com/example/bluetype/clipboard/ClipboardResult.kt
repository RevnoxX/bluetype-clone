package com.example.bluetype.clipboard

/**
 * ClipboardResult — Models the outcome of attempting to read the system clipboard.
 *
 * Responsibility: Categorizes clipboard contents into plain text, sensitive text, empty, or unreadable.
 * Depends on: None.
 * Notes: Encapsulates Android 13+ EXTRA_IS_SENSITIVE flag per §9 security requirements.
 */
sealed class ClipboardResult {

    /**
     * Successfully read non-sensitive plain text.
     *
     * @property text The plain text payload.
     */
    data class Text(val text: String) : ClipboardResult()

    /**
     * Successfully read text flagged as sensitive (e.g., password manager payload).
     * Requires explicit user confirmation before transmission per §9.
     *
     * @property text The sensitive text payload.
     */
    data class Sensitive(val text: String) : ClipboardResult()

    /**
     * The system clipboard is empty.
     */
    object Empty : ClipboardResult()

    /**
     * The clipboard contains non-text data (e.g., image, URI, or audio).
     *
     * @property reason Plain-language explanation for why it cannot be converted to keystrokes.
     */
    data class Unreadable(val reason: String) : ClipboardResult()
}
