package com.gasmonsoft.fuelboxcontrol.data.model.ble

import java.util.UUID

val SERVICE_FIRMWARE_UUID = UUID.fromString("958ae39e-2fb0-4389-9e63-0c68cb134426")

val CONTROL_FIRMWARE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ac")

val DATA_FIRMWARE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ad")

val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

val CHAR_UUID_SENSOR_1 = UUID.fromString("09e0330f-dd3f-45a0-86e7-38551f0552b2")
val CHAR_UUID_SENSOR_2 = UUID.fromString("b47e0770-340c-44fd-88db-718b2168ed36")
val CHAR_UUID_SENSOR_3 = UUID.fromString("4e17aa50-fd60-4b0e-86af-4a0bca00dc47")
val CHAR_UUID_SENSOR_4 = UUID.fromString("fc128c3e-7587-4db2-9b87-8dca799fb5b3")
val CHAR_UUID_ACELEROMETRO = UUID.fromString("13728dec-2e97-4ffc-8d56-6c04f2ddd796")
val CHAR_UUID_ALERTAS_GLOBALES = UUID.fromString("80c4c443-2128-4570-b0da-6b3dbced01a6")

fun safeUuidOrNull(uuidStr: String?): UUID? {
    val s = uuidStr?.trim().orEmpty()
    if (s.isBlank()) return null
    return runCatching { UUID.fromString(s) }.getOrNull()
}