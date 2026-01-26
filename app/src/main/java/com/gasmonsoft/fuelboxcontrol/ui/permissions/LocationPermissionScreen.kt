package com.gasmonsoft.fuelboxcontrol.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun LocationPermissionScreen(onGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onGranted()
    }

    PermissionScaffold(
        title = "Permiso de ubicación",
        description = "Android requiere este permiso para detectar dispositivos Bluetooth cercanos. No rastreamos tu ubicación."
    ) {
        Button(
            onClick = {
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        ) {
            Text("Permitir ubicación")
        }
    }
}