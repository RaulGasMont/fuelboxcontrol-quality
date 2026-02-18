package com.gasmonsoft.fuelboxcontrol.ui

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.gasmonsoft.fuelboxcontrol.ui.permissions.PermissionScaffold

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    PermissionScaffold(
        title = "Necesitamos algunos permisos",
        description = "Para conectarnos a dispositivos Bluetooth y funcionar correctamente."
    ) {
        Button(onClick = onContinue) {
            Text("Continuar")
        }
    }
}