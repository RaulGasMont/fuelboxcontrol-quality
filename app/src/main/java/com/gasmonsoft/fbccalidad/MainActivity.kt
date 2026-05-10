package com.gasmonsoft.fbccalidad

import android.content.Intent
import android.content.pm.ActivityInfo
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gasmonsoft.fbccalidad.domain.usb.UsbConnectionMonitor
import com.gasmonsoft.fbccalidad.ui.mainnav.screen.FuelBoxControlFlowNav
import com.gasmonsoft.fbccalidad.ui.theme.FuelBoxControlTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var usbConnectionMonitor: UsbConnectionMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        //usbConnectionMonitor.startMonitoring()

        setContent {
            FuelBoxControlTheme {
                FuelBoxControlFlowNav()
            }
        }

        handleUsbIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleUsbIntent(intent)
    }

    private fun handleUsbIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            device?.let { usbConnectionMonitor.onDeviceAttached(it) }
        }
    }
}