package com.gasmonsoft.fuelboxcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.gasmonsoft.fuelboxcontrol.ui.SensorScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorScreen(
                nameWifi = "",
                passWifi = "",
            )
        }
    }
}