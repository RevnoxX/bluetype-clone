package com.example.bluetype.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.bluetype.BlueTypeApplication
import com.example.bluetype.MainActivity
import com.example.bluetype.hid.ConnectionState
import com.example.bluetype.ui.TrampolineActivity

/**
 * SendClipboardTileService — Quick Settings Tile for one-tap clipboard transmission.
 *
 * Responsibility: Exposes system Quick Settings Tile reflecting connection state and launching TrampolineActivity.
 * Depends on: [TileService], [TrampolineActivity], [MainActivity], [BlueTypeApplication].
 * Notes: Launches TrampolineActivity to briefly acquire input focus before reading clipboard (§4.2).
 */
class SendClipboardTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val app = application as? BlueTypeApplication
        val transport = app?.container?.hidTransport
        val isConnected = transport?.connectionState?.value is ConnectionState.Connected

        if (isConnected) {
            val intent = Intent(this, TrampolineActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val app = application as? BlueTypeApplication
        val transport = app?.container?.hidTransport
        val state = transport?.connectionState?.value

        if (state is ConnectionState.Connected) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Send to ${state.deviceName}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Connected"
            }
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Send Clipboard"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = "Disconnected"
            }
        }
        tile.updateTile()
    }
}
