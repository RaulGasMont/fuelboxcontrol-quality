package com.gasmonsoft.fuelboxcontrol.data.ble

import java.util.UUID

val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

fun safeUuidOrNull(uuidStr: String?): UUID? {
    val s = uuidStr?.trim().orEmpty()
    if (s.isBlank()) return null
    return runCatching { UUID.fromString(s) }.getOrNull()
}