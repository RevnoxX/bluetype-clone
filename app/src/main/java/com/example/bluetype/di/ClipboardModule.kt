package com.example.bluetype.di

import android.content.Context
import com.example.bluetype.clipboard.ClipboardReader
import com.example.bluetype.clipboard.FocusedClipboardReader

/**
 * ClipboardModule — Provider module for ClipboardReader implementation.
 *
 * Responsibility: Resolves ClipboardReader to FocusedClipboardReader.
 * Depends on: [Context], [ClipboardReader], [FocusedClipboardReader].
 * Notes: Binds reader implementation bound to focused Activity windows (§6).
 */
object ClipboardModule {
    fun provideClipboardReader(context: Context): ClipboardReader {
        return FocusedClipboardReader(context)
    }
}
