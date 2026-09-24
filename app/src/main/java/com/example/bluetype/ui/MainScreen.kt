package com.example.bluetype.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetype.R
import com.example.bluetype.hid.ConnectionState
import com.example.bluetype.service.HidForegroundService
import com.example.bluetype.ui.components.BluetoothDeviceItem
import com.example.bluetype.ui.components.ConnectedDeviceHeader
import com.example.bluetype.ui.limitations.LimitationsDialog
import com.example.bluetype.ui.onboarding.OnboardingSheet
import com.example.bluetype.ui.settings.SettingsSheet
import com.example.bluetype.ui.theme.ConnectedGreen
import com.example.bluetype.viewmodel.MainViewModel
import kotlinx.coroutines.launch

import android.bluetooth.BluetoothDevice
import com.example.bluetype.ui.components.DeviceInfoSheet

/**
 * MainScreen — Primary user interface for BlueType.
 *
 * Responsibility: Hosts connected device header, device discovery LazyColumn, settings, and clipboard FAB.
 * Depends on: [MainViewModel], [ConnectedDeviceHeader], [BluetoothDeviceItem], [DeviceInfoSheet].
 * Notes: Implements clean, professional minimal layout per user specification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showSettings by remember { mutableStateOf(false) }
    var showLimitations by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }

    var infoDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var infoDeviceName by remember { mutableStateOf("") }
    var infoDeviceAddress by remember { mutableStateOf("") }
    var infoDeviceBonded by remember { mutableStateOf(false) }
    var infoDeviceRssi by remember { mutableStateOf<Short?>(null) }

    // Start/stop foreground service when settings or connection changes
    LaunchedEffect(settings.showPersistentNotification, uiState.connectionState) {
        if (settings.showPersistentNotification && uiState.connectionState is ConnectionState.Connected) {
            HidForegroundService.startService(context)
        }
    }

    // Display feedback message in snackbar
    LaunchedEffect(uiState.feedbackMessage) {
        uiState.feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.connectionState is ConnectionState.Connected) {
                                        ConnectedGreen
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLimitations = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = stringResource(R.string.cd_open_limitations)
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_open_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            val isConnected = uiState.connectionState is ConnectionState.Connected
            ExtendedFloatingActionButton(
                onClick = { viewModel.onSendClipboardClicked() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.ContentPasteGo,
                        contentDescription = stringResource(R.string.cd_fab_paste)
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.fab_paste_to_pc),
                        fontWeight = FontWeight.Bold
                    )
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = if (isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isConnected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Top Section: Connected Device Header + Switcher
            ConnectedDeviceHeader(
                connectionState = uiState.connectionState,
                onDisconnect = { viewModel.disconnectDevice() },
                onSwitchDevicesClicked = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(1)
                    }
                }
            )

            // 2. Active Typing Progress Bar
            AnimatedVisibility(
                visible = uiState.isSending,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                val progress = uiState.sendProgress
                val fraction = if (progress != null && progress.second > 0) {
                    progress.first.toFloat() / progress.second.toFloat()
                } else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.sending_keystrokes,
                                progress?.first ?: 0,
                                progress?.second ?: 0
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            // 3. Performant Device Discovery LazyColumn
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Section: Bonded / Paired Devices
                item(key = "header_bonded") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.header_bonded_devices),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(
                            onClick = { viewModel.refreshBondedDevices() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (uiState.bondedDevices.isEmpty()) {
                    item(key = "empty_bonded") {
                        Text(
                            text = stringResource(R.string.no_bonded_devices),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(
                        items = uiState.bondedDevices,
                        key = { "bonded_${it.address}" }
                    ) { device ->
                        val isConnected = uiState.connectedDevice?.address == device.address &&
                                uiState.connectionState is ConnectionState.Connected
                        val isConnecting = uiState.connectionState is ConnectionState.Connecting &&
                                (uiState.connectionState as ConnectionState.Connecting).device?.address == device.address

                        val devName = try { device.name ?: device.address } catch (e: SecurityException) { device.address }
                        BluetoothDeviceItem(
                            name = devName,
                            address = device.address,
                            isBonded = true,
                            isConnected = isConnected,
                            isConnecting = isConnecting,
                            onConnect = { viewModel.connectDevice(device) },
                            onDisconnect = { viewModel.disconnectDevice() },
                            onInfoClick = {
                                infoDevice = device
                                infoDeviceName = devName
                                infoDeviceAddress = device.address
                                infoDeviceBonded = true
                                infoDeviceRssi = null
                            }
                        )
                    }
                }

                // Section: Nearby Discovered Devices
                item(key = "header_nearby") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.header_nearby_devices),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        FilledTonalButton(
                            onClick = {
                                if (uiState.isScanning) {
                                    viewModel.stopScan()
                                } else {
                                    viewModel.startScan()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            if (uiState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_stop_scan),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.action_scan),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                val unbondedNearby = uiState.nearbyDevices.filter { !it.isBonded }

                if (unbondedNearby.isEmpty()) {
                    item(key = "empty_nearby") {
                        Text(
                            text = stringResource(R.string.no_nearby_devices),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(
                        items = unbondedNearby,
                        key = { "nearby_${it.address}" }
                    ) { discovered ->
                        val isConnected = uiState.connectedDevice?.address == discovered.address &&
                                uiState.connectionState is ConnectionState.Connected
                        val isConnecting = uiState.connectionState is ConnectionState.Connecting &&
                                (uiState.connectionState as ConnectionState.Connecting).device?.address == discovered.address

                        BluetoothDeviceItem(
                            name = discovered.name,
                            address = discovered.address,
                            isBonded = discovered.isBonded,
                            isConnected = isConnected,
                            isConnecting = isConnecting,
                            onConnect = { viewModel.connectDevice(discovered.device) },
                            onDisconnect = { viewModel.disconnectDevice() },
                            onInfoClick = {
                                infoDevice = discovered.device
                                infoDeviceName = discovered.name
                                infoDeviceAddress = discovered.address
                                infoDeviceBonded = discovered.isBonded
                                infoDeviceRssi = discovered.rssi
                            }
                        )
                    }
                }
            }
        }
    }

    // Sensitive Content Confirmation Dialog (§9)
    if (uiState.pendingSensitiveText != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSensitivePrompt() },
            title = {
                Text(
                    text = stringResource(R.string.sensitive_clip_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.sensitive_clip_message))
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSensitiveSend() }) {
                    Text(
                        stringResource(R.string.action_send),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissSensitivePrompt() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Settings Sheet
    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onDismiss = { showSettings = false },
            onTypingDelayChange = { viewModel.setTypingDelay(it) },
            onSkipUnsupportedChange = { viewModel.setSkipUnsupported(it) },
            onReplacementCharChange = { viewModel.setReplacementChar(it) },
            onShowNotificationChange = { viewModel.setShowNotification(it) },
            onAutoClearChange = { viewModel.setAutoClear(it) }
        )
    }

    // Limitations Dialog
    if (showLimitations) {
        LimitationsDialog(onDismiss = { showLimitations = false })
    }

    // Onboarding / Permissions Sheet
    if (showPermissions) {
        OnboardingSheet(
            onDismiss = { showPermissions = false },
            onRequestPermissions = onRequestPermissions
        )
    }

    // Device Info Bottom Sheet / Drawer
    if (infoDevice != null) {
        val currentConnected = uiState.connectedDevice?.address == infoDeviceAddress &&
                uiState.connectionState is ConnectionState.Connected
        DeviceInfoSheet(
            device = infoDevice!!,
            name = infoDeviceName,
            address = infoDeviceAddress,
            isBonded = infoDeviceBonded,
            isConnected = currentConnected,
            rssi = infoDeviceRssi,
            onDismiss = { infoDevice = null },
            onConnect = { viewModel.connectDevice(infoDevice!!) },
            onDisconnect = { viewModel.disconnectDevice() }
        )
    }
}
