package com.gasmonsoft.fuelboxcontrol.ui.vehiculo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun VehiculosRoute(
    modifier: Modifier = Modifier,
    viewModel: VehiculosViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Vehiculos")
    }
}

@Preview
@Composable
fun VehiculosRoutePreview() {
    VehiculosRoute()
}

