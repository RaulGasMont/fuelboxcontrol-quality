package com.gasmonsoft.fbccalidad.data.model.ble

sealed interface ConnectionState {
    object Connected : ConnectionState
    object Disconnected : ConnectionState
    object Uninitialized : ConnectionState
    object CurrentlyInitializing : ConnectionState
}