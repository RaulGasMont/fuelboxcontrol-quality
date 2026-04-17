package com.gasmonsoft.fbccalidad

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gasmonsoft.fbccalidad.ui.mainnav.screen.FuelBoxControlFlowNav
import com.gasmonsoft.fbccalidad.ui.theme.FuelBoxControlTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContent {
            FuelBoxControlTheme {
                FuelBoxControlFlowNav()
            }
        }
    }
}