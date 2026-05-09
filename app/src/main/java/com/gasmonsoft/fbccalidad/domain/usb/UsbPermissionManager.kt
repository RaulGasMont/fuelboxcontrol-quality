package com.gasmonsoft.fbccalidad.domain.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsbPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager
) {

    private val ACTION_USB_PERMISSION = "com.rfz.USB_PERMISSION"

    private val _permissionState = MutableSharedFlow<UsbPermissionResult>(
        extraBufferCapacity = 1
    )
    val permissionState: Flow<UsbPermissionResult> = _permissionState.asSharedFlow()

    private var permissionReceiver: BroadcastReceiver? = null

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun requestPermission(device: UsbDevice) {
        Log.d("UsbPermission", "Requesting permission for: ${device.deviceName}")
        // Ya tiene permiso — no hace falta pedir
        if (usbManager.hasPermission(device)) {
            Log.d("UsbPermission", "Already has permission for: ${device.deviceName}")
            _permissionState.tryEmit(UsbPermissionResult.Granted(device))
            return
        }

        // Registra receptor de respuesta
        permissionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d("UsbPermission", "OnReceive permission broadcast: ${intent.action}")
                if (intent.action != ACTION_USB_PERMISSION) return

                val grantedDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                    ?: return
                val granted = intent.getBooleanExtra(
                    UsbManager.EXTRA_PERMISSION_GRANTED, false
                )

                Log.d("UsbPermission", "Permission result for ${grantedDevice.deviceName}: Granted=$granted")

                val result = if (granted) {
                    UsbPermissionResult.Granted(grantedDevice)
                } else {
                    UsbPermissionResult.Denied(grantedDevice)
                }

                _permissionState.tryEmit(result)
                unregisterReceiver()
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else 0

        context.registerReceiver(
            permissionReceiver,
            IntentFilter(ACTION_USB_PERMISSION),
            flags
        )

        Log.d("UsbPermission", "Launching system permission dialog...")
        // Lanza el diálogo al usuario
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        usbManager.requestPermission(device, pendingIntent)
    }

    private fun unregisterReceiver() {
        permissionReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
            permissionReceiver = null
        }
    }
}

sealed class UsbPermissionResult {
    data class Granted(val device: UsbDevice) : UsbPermissionResult()
    data class Denied(val device: UsbDevice) : UsbPermissionResult()
}