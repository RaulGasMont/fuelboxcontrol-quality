package com.gasmonsoft.fuelboxcontrol.ui.permissions

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PermissionResultScreen(onFinish: () -> Unit) {
    PermissionScaffold(
        title = "¡Todo listo!",
        description = "Los permisos necesarios han sido configurados correctamente."
    ) {
        Button(onClick = onFinish) {
            Text("Comenzar")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionResultScreenPreview() {
    PermissionResultScreen(onFinish = {})
}