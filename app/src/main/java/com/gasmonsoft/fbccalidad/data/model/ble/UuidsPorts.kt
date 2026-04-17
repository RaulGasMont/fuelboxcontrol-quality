package com.gasmonsoft.fbccalidad.data.model.ble

import java.util.UUID

val SERVICE_FIRMWARE_UUID = UUID.fromString("958ae39e-2fb0-4389-9e63-0c68cb134426")

val CONTROL_FIRMWARE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ac")

val DATA_FIRMWARE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ad")

val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

val CHAR_UUID_SENSOR_1 = UUID.fromString("09e0330f-dd3f-45a0-86e7-38551f0552b2")

fun safeUuidOrNull(uuidStr: String?): UUID? {
    val s = uuidStr?.trim().orEmpty()
    if (s.isBlank()) return null
    return runCatching { UUID.fromString(s) }.getOrNull()
}