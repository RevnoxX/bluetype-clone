package com.example.bluetype.ui.components

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetype.R
import com.example.bluetype.ui.theme.ConnectedGreen

/**
 * DeviceInfoSheet — Modal bottom sheet displaying detailed hardware and Bluetooth specifications for a device.
 *
 * Responsibility: Renders comprehensive device metadata including MAC address, bond state, class, and RSSI.
 * Depends on: [BluetoothDevice], Material 3 ModalBottomSheet.
 * Notes: Satisfies the requirement: "info button that shows up a drawer that shows up all the information about the device like mac address etc."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoSheet(
    device: BluetoothDevice,
    name: String,
    address: String,
    isBonded: Boolean,
    isConnected: Boolean,
    rssi: Short?,
    onDismiss: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bondStateText = when (device.bondState) {
        BluetoothDevice.BOND_BONDED -> "Paired (Bonded)"
        BluetoothDevice.BOND_BONDING -> "Pairing in progress..."
        else -> "Unpaired"
    }

    val deviceTypeText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic (BR/EDR)"
            BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy (BLE)"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode (BR/EDR + BLE)"
            else -> "Standard Bluetooth"
        }
    } else {
        "Classic Bluetooth"
    }

    val deviceClassText = try {
        val btClass = device.bluetoothClass
        when (btClass?.majorDeviceClass) {
            BluetoothClass.Device.Major.COMPUTER -> "Computer / Desktop PC"
            BluetoothClass.Device.Major.PHONE -> "Phone"
            BluetoothClass.Device.Major.PERIPHERAL -> "Keyboard / Mouse / Peripheral"
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio / Video Device"
            BluetoothClass.Device.Major.WEARABLE -> "Wearable"
            else -> "Bluetooth Endpoint (0x${Integer.toHexString(btClass?.deviceClass ?: 0)})"
        }
    } catch (e: SecurityException) {
        "Bluetooth Device"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isLikelyPc = name.contains("DESKTOP", ignoreCase = true) ||
                            name.contains("PC", ignoreCase = true) ||
                            name.contains("LAPTOP", ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLikelyPc) Icons.Default.Computer else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = name.ifBlank { "Unknown Device" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) "Connected as HID Keyboard" else "Disconnected",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) ConnectedGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Information Cards
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // MAC Address with Copy
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoRowItem(
                            icon = Icons.Default.Fingerprint,
                            label = "Hardware MAC Address",
                            value = address,
                            isMonospace = true
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MAC Address", address))
                                Toast.makeText(context, "Copied MAC address", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy MAC Address",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bond state
                    InfoRowItem(
                        icon = Icons.Default.Link,
                        label = "Pairing Status",
                        value = bondStateText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Device Type
                    InfoRowItem(
                        icon = Icons.Default.Devices,
                        label = "Bluetooth Architecture",
                        value = deviceTypeText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Device Class
                    InfoRowItem(
                        icon = Icons.Default.Computer,
                        label = "Device Class",
                        value = deviceClassText
                    )

                    if (rssi != null && rssi != Short.MIN_VALUE) {
                        Spacer(modifier = Modifier.height(14.dp))
                        InfoRowItem(
                            icon = Icons.Default.SignalCellularAlt,
                            label = "Signal Strength (RSSI)",
                            value = "$rssi dBm"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // HID Profile Role
                    InfoRowItem(
                        icon = Icons.Default.NetworkCheck,
                        label = "HID Profile Support",
                        value = "Standard HID Host (Windows PC)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Close")
                }

                if (isConnected) {
                    FilledTonalButton(
                        onClick = {
                            onDisconnect()
                            onDismiss()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disconnect")
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
                            onConnect()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Connect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                fontSize = if (isMonospace) 13.sp else 14.sp
            )
        }
    }
}
