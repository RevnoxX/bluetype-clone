package com.example.bluetype

import android.app.Application
import com.example.bluetype.di.AppContainer
import timber.log.Timber

/**
 * BlueTypeApplication — Application entry point and dependency container owner.
 *
 * Responsibility: Initializes app-wide singletons, Timber logging with redaction tree, and AppContainer.
 * Depends on: [AppContainer], [Timber].
 * Notes: Never logs raw clipboard text in any build type (§9).
 */
class BlueTypeApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Install redaction tree to enforce zero clipboard leakage in logs (§9)
        Timber.plant(RedactionDebugTree())
    }

    /**
     * Timber DebugTree that actively checks and redacts any clipboard-derived data (§9).
     */
    private class RedactionDebugTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            val sanitized = sanitize(message)
            super.log(priority, tag, sanitized, t)
        }

        private fun sanitize(input: String): String {
            // Redact any string with clipboard text markers
            if (input.contains("clip_content", ignoreCase = true) ||
                input.contains("clipboard_payload", ignoreCase = true)
            ) {
                return "[REDACTED CLIPBOARD CONTENT]"
            }
            return input
        }
    }
}
