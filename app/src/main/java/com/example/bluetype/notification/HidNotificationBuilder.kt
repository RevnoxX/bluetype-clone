package com.example.bluetype.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bluetype.MainActivity
import com.example.bluetype.R
import com.example.bluetype.hid.ConnectionState
import com.example.bluetype.ui.TrampolineActivity

/**
 * HidNotificationBuilder — Builds persistent connection status notifications.
 *
 * Responsibility: Creates and updates the foreground service notification and its "Send Clipboard" action.
 * Depends on: [Context], [ConnectionState], [MainActivity], [TrampolineActivity].
 * Notes: Notification action triggers TrampolineActivity to satisfy Android 10+ clipboard focus requirement (§4.2).
 */
class HidNotificationBuilder(
    private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "bluetype_connection_channel"
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_CODE_MAIN = 2001
        private const val REQUEST_CODE_TRAMPOLINE = 2002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds the persistent notification reflecting the current [ConnectionState].
     *
     * @param state The active HID connection state.
     * @return Prepared [Notification] instance.
     */
    fun buildNotification(state: ConnectionState): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_MAIN,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sendClipboardIntent = Intent(context, TrampolineActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val sendClipboardPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_TRAMPOLINE,
            sendClipboardIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val content: String

        when (state) {
            is ConnectionState.Connected -> {
                title = context.getString(R.string.notif_title_connected, state.deviceName)
                content = context.getString(R.string.notif_content_ready)
            }
            is ConnectionState.Connecting -> {
                title = context.getString(R.string.notif_title_connecting)
                content = state.deviceName
            }
            is ConnectionState.Unsupported -> {
                title = context.getString(R.string.notif_title_unsupported)
                content = state.reason
            }
            is ConnectionState.Disconnected -> {
                title = context.getString(R.string.notif_title_disconnected)
                content = state.reason ?: context.getString(R.string.notif_content_disconnected)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        // Only show "Send Clipboard" action when actively connected to a host PC
        if (state is ConnectionState.Connected) {
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                context.getString(R.string.action_send_clipboard),
                sendClipboardPendingIntent
            )
        }

        return builder.build()
    }
}
