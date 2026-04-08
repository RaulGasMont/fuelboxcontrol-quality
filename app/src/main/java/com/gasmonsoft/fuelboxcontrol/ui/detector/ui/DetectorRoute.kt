package com.gasmonsoft.fuelboxcontrol.ui.detector.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme

@Composable
fun DetectorRoute(modifier: Modifier = Modifier) {
    DetectorScreen(modifier = modifier)
}

@Composable
fun DetectorScreen(modifier: Modifier = Modifier) {

}


@Preview()
@Composable
fun DetectorScreenPreview() {
    FuelBoxControlTheme {
        DetectorScreen()
    }
}