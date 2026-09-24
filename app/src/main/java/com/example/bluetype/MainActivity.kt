package com.example.bluetype

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.example.bluetype.ui.MainScreen
import com.example.bluetype.ui.theme.BluetypeTheme
import com.example.bluetype.viewmodel.MainViewModel

/**
 * MainActivity — Host activity for the BlueType Compose UI.
 *
 * Responsibility: Sets up Compose content, requests runtime permissions, and wires MainViewModel.
 * Depends on: [MainViewModel], [MainScreen], [BlueTypeApplication].
 * Notes: Acts as the primary focused Activity window for user-triggered clipboard operations (§4.2).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as BlueTypeApplication
        MainViewModel.Factory(
            hidTransport = app.container.hidTransport,
            bluetoothScanner = app.container.bluetoothScanner,
            clipboardReader = app.container.clipboardReader,
            sendClipboardUseCase = app.container.sendClipboardUseCase,
            settingsRepository = app.container.settingsRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val connectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: true
                if (connectGranted) {
                    val app = application as BlueTypeApplication
                    app.container.hidTransport.register()
                    app.container.hidTransport.refreshBondedDevices()
                }
            }

            fun requestRequiredPermissions() {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                } else {
                    permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionsLauncher.launch(permissionsToRequest.toTypedArray())
            }

            LaunchedEffect(Unit) {
                requestRequiredPermissions()
            }

            BluetypeTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestPermissions = { requestRequiredPermissions() }
                )
            }
        }
    }
}