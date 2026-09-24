package com.example.bluetype.ui.components

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetype.R
import com.example.bluetype.hid.ConnectionState
import com.example.bluetype.ui.theme.ConnectedGreen
import com.example.bluetype.ui.theme.ConnectingAmber
import com.example.bluetype.ui.theme.DisconnectedGray
import com.example.bluetype.ui.theme.ErrorRed

/**
 * ConnectedDeviceHeader — Top card rendering active host PC status and device switcher.
 *
 * Responsibility: Displays connected PC information and secondary button to switch/connect to other devices.
 * Depends on: [ConnectionState], [BluetoothDevice].
 * Notes: Satisfies the primary UI requirement: "At the top show the connected device Right side to it show option to connect to other devices".
 */
@Composable
fun ConnectedDeviceHeader(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    onSwitchDevicesClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ConnectionState.Connected -> ConnectedGreen
            is ConnectionState.Connecting -> ConnectingAmber
            is ConnectionState.Unsupported -> ErrorRed
            is ConnectionState.Disconnected -> DisconnectedGray
        },
        label = "statusColorAnimation"
    )

    val isConnected = connectionState is ConnectionState.Connected
    val shimmerBrush = if (isConnected) {
        rememberShimmerBrush(
            shimmerColor = ConnectedGreen.copy(alpha = 0.28f),
            durationMs = 2200
        )
    } else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        ),
        border = BorderStroke(
            if (isConnected) 1.5.dp else 1.dp,
            if (isConnected) ConnectedGreen.copy(alpha = 0.75f) else statusColor.copy(alpha = 0.4f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (shimmerBrush != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(shimmerBrush)
                )
            }
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Status badge & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = statusColor,
                            modifier = Modifier.size(10.dp)
                        ) {}
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.header_connected_device),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }

                // Right side: Switch / Other devices button
                FilledTonalButton(
                    onClick = onSwitchDevicesClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = stringResource(R.string.switch_devices),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.switch_devices),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Content: Host info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (connectionState) {
                            is ConnectionState.Connected -> Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                            is ConnectionState.Connecting -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = statusColor
                            )
                            is ConnectionState.Unsupported -> Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                            is ConnectionState.Disconnected -> Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val titleText = when (connectionState) {
                        is ConnectionState.Connected -> connectionState.deviceName
                        is ConnectionState.Connecting -> connectionState.deviceName
                        is ConnectionState.Unsupported -> stringResource(R.string.device_status_unsupported)
                        is ConnectionState.Disconnected -> stringResource(R.string.no_device_connected)
                    }

                    val subtitleText = when (connectionState) {
                        is ConnectionState.Connected -> stringResource(R.string.device_status_connected)
                        is ConnectionState.Connecting -> stringResource(R.string.device_status_connecting)
                        is ConnectionState.Unsupported -> connectionState.reason
                        is ConnectionState.Disconnected -> connectionState.reason ?: stringResource(R.string.device_status_disconnected)
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Disconnect action when connected
                if (connectionState is ConnectionState.Connected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_disconnect),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        }
    }
}
