package com.gasmonsoft.fuelboxcontrol.data.service.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleConnectionManager @Inject constructor() {
    @Volatile
    private var connectionState: Int = BluetoothProfile.STATE_DISCONNECTED
    @Volatile
    private var bluetoothGatt: BluetoothGatt? = null
    @Volatile
    private var currentMac: String = ""

    fun setGatt(gatt: BluetoothGatt, mac: String) {
        bluetoothGatt = gatt
        currentMac = mac
    }

    fun getGatt(): BluetoothGatt? = bluetoothGatt
    fun getMac(): String = currentMac

    fun isConnected(): Boolean = connectionState == BluetoothProfile.STATE_CONNECTED
    fun setStatus(bleStatus: Int) {
        connectionState = bleStatus
    }

    fun clearGatt() {
        connectionState = BluetoothProfile.STATE_DISCONNECTED
        bluetoothGatt = null
        currentMac = ""
    }

}