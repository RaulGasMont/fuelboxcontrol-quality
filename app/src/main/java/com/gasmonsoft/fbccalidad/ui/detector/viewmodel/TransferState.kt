package com.gasmonsoft.fbccalidad.ui.detector.viewmodel

import android.hardware.usb.UsbDevice

sealed class TransferState {
    object Idle : TransferState()
    data class DeviceDetected(val device: UsbDevice) : TransferState()
    object RequestingPermission : TransferState()
    data class DeviceReady(val device: UsbDevice) : TransferState()
    object Connecting : TransferState()
    object Reading : TransferState()
    object Success : TransferState()
    data class Error(val message: String) : TransferState()
}