package com.gasmonsoft.fuelboxcontrol.data.model.ble

sealed interface ConnectionState {
    object Connected : ConnectionState
    object Disconnected : ConnectionState
    object Uninitialized : ConnectionState
    object CurrentlyInitializing : ConnectionState
}