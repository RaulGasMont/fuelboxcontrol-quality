package com.gasmonsoft.fbccalidad.domain.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbConnectionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager
) {

    private val _deviceState = MutableStateFlow<UsbDeviceState>(UsbDeviceState.Disconnected)
    val deviceState: StateFlow<UsbDeviceState> = _deviceState.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                ?: return

            Log.d("UsbMonitor", "onReceive: ${intent.action} - Device: ${device.deviceName}")

            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d("UsbMonitor", "USB Device Attached: ${device.deviceName}")
                    _deviceState.value = UsbDeviceState.Attached(device)
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d("UsbMonitor", "USB Device Detached")
                    _deviceState.value = UsbDeviceState.Disconnected
                }
            }
        }
    }

    private var isMonitoring = false

    fun startMonitoring() {
        if (isMonitoring) {
            checkAlreadyConnected()
            return
        }

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        isMonitoring = true
        checkAlreadyConnected()
    }

    /**
     * Permite a la Activity notificar manualmente cuando recibe un Intent de USB ATTACHED
     */
    fun onDeviceAttached(device: UsbDevice) {
        Log.d("UsbMonitor", "Manual Device Attached Notification: ${device.deviceName}")
        _deviceState.value = UsbDeviceState.Attached(device)
    }

    fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        runCatching { context.unregisterReceiver(receiver) }
    }

    private fun checkAlreadyConnected() {
        val devices = usbManager.deviceList
        Log.d("UsbMonitor", "Checking for already connected devices. Found: ${devices.size}")

        val device = devices.values.firstOrNull()
        if (device != null) {
            Log.d("UsbMonitor", "Found already connected device: ${device.deviceName}")
            _deviceState.value = UsbDeviceState.Attached(device)
        } else {
            Log.d("UsbMonitor", "No USB devices found currently connected.")
        }
    }
}

sealed class UsbDeviceState {
    object Disconnected : UsbDeviceState()
    data class Attached(val device: UsbDevice) : UsbDeviceState()
    data class Ready(val device: UsbDevice) : UsbDeviceState()     // permiso concedido
    data class Denied(val device: UsbDevice) : UsbDeviceState()    // permiso denegado
}