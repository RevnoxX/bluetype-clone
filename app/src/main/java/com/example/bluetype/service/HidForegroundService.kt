package com.example.bluetype.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.bluetype.BlueTypeApplication
import com.example.bluetype.notification.HidNotificationBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * HidForegroundService — Keeps Bluetooth HID connection alive and updates persistent notification.
 *
 * Responsibility: Owns the foreground service lifecycle with foregroundServiceType="connectedDevice".
 * Depends on: [HidTransport], [HidNotificationBuilder], [BlueTypeApplication].
 * Notes: Explicit foreground service type declared for Android 14+ compliance (§2.3).
 */
class HidForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.example.bluetype.action.START"
        const val ACTION_STOP = "com.example.bluetype.action.STOP"

        fun startService(context: Context) {
            val intent = Intent(context, HidForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, HidForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationBuilder: HidNotificationBuilder

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = HidNotificationBuilder(this)
        val app = application as? BlueTypeApplication
        val transport = app?.container?.hidTransport

        if (transport != null) {
            serviceScope.launch {
                transport.connectionState.collectLatest { state ->
                    val notification = notificationBuilder.buildNotification(state)
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(HidNotificationBuilder.NOTIFICATION_ID, notification)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val app = application as? BlueTypeApplication
        val transport = app?.container?.hidTransport
        val currentState = transport?.connectionState?.value
            ?: com.example.bluetype.hid.ConnectionState.Disconnected()

        val notification = notificationBuilder.buildNotification(currentState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                HidNotificationBuilder.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(HidNotificationBuilder.NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
