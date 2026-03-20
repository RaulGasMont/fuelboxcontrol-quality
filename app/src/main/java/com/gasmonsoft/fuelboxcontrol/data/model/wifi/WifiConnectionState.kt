package com.gasmonsoft.fuelboxcontrol.data.model.wifi

data class WifiConnectionState(
    val connected: Boolean = false,
    val wifi: Boolean = false,
    val validated: Boolean = false,
    val ssid: String? = null,
    val rssi: Int? = null,
    val signalLevel: Int? = null, // 0..4
    val signalMessage: String = ""
)