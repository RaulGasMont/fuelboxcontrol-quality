package com.gasmonsoft.fuelboxcontrol.utils

import kotlinx.coroutines.flow.MutableStateFlow


object NetworkConfig {

    val _deleteAllTasksCompleted = MutableStateFlow(false)


    var SENSOR1_SERVICE_UIID = ""
    var Volumen1_CHARACTERISTICS_UUID = ""
    var Temperatura1_CHARACTERISTICS_UUID = ""
    var Constante1_CHARACTERISTICS_UUID = ""
    var Fecha1_CHARACTERISTICS_UUID = ""
    var Alertas1_CHARACTERISTICS_UUID = ""
    var Notificacion1_DESCRIPTOR_UUID = ""
    var SensorId1 = ""
    var mac1 = ""

    var SENSOR2_SERVICE_UIID = ""
    var Volumen2_CHARACTERISTICS_UUID = ""
    var Temperatura2_CHARACTERISTICS_UUID = ""
    var Constante2_CHARACTERISTICS_UUID = ""
    var Fecha2_CHARACTERISTICS_UUID = ""
    var Alertas2_CHARACTERISTICS_UUID = ""
    var Notificacion2_DESCRIPTOR_UUID = ""
    var SensorId2 = ""
    var mac2 = ""

    var SENSOR3_SERVICE_UIID = ""
    var Volumen3_CHARACTERISTICS_UUID = ""
    var Temperatura3_CHARACTERISTICS_UUID = ""
    var Constante3_CHARACTERISTICS_UUID = ""
    var Fecha3_CHARACTERISTICS_UUID = ""
    var Alertas3_CHARACTERISTICS_UUID = ""
    var Notificacion3_DESCRIPTOR_UUID = ""
    var SensorId3 = ""
    var mac3 = ""

    var idcajasensor = ""
    var SENSOR4_SERVICE_UIID = ""
    var Volumen4_CHARACTERISTICS_UUID = ""
    var Temperatura4_CHARACTERISTICS_UUID = ""
    var Constante4_CHARACTERISTICS_UUID = ""
    var Fecha4_CHARACTERISTICS_UUID = ""
    var Alertas4_CHARACTERISTICS_UUID = ""
    var Notificacion4_DESCRIPTOR_UUID = ""
    var SensorId4 = ""
    var mac4 = ""

    var ControlService_SERVICE_UUID = ""
    var Bateria_CHARACTERISTICS_UUID = ""
    var Senial_CHARACTERISTICS_UUID = ""
    var Comandos_CHARACTERISTICS_UUID = ""
    var AlertasG_CHARACTERISTICS_UUID = ""
    var NotificacionControl_DESCRIPTOR_UUID = ""
    var configuracion = ""
    var nombreconfiguracion = ""

    fun setSensorServiceUUID(index: Int, uuid: String) {
        when (index) {
            1 -> SENSOR1_SERVICE_UIID = uuid
            2 -> SENSOR2_SERVICE_UIID = uuid
            3 -> SENSOR2_SERVICE_UIID = uuid
            4 -> SENSOR4_SERVICE_UIID = uuid
        }
    }


    fun isServiceUUIDDesired(uuid: String): Boolean {
        return uuid == SENSOR1_SERVICE_UIID ||
                uuid == SENSOR2_SERVICE_UIID ||
                uuid == SENSOR3_SERVICE_UIID ||
                uuid == SENSOR4_SERVICE_UIID
    }

    fun setVolumenCharacteristicsUUID(index: Int, uuid: String) {
        when (index) {
            1 -> Volumen1_CHARACTERISTICS_UUID = uuid
            2 -> Volumen2_CHARACTERISTICS_UUID = uuid
            3 -> Volumen3_CHARACTERISTICS_UUID = uuid
            4 -> Volumen4_CHARACTERISTICS_UUID = uuid
        }
    }


    fun setTemperaturaCharacteristicsUUID(index: Int, uuid: String) {
        when (index) {
            1 -> Temperatura1_CHARACTERISTICS_UUID = uuid
            2 -> Temperatura2_CHARACTERISTICS_UUID = uuid
            3 -> Temperatura3_CHARACTERISTICS_UUID = uuid
            4 -> Temperatura4_CHARACTERISTICS_UUID = uuid
        }
    }

    fun setConstanteCharacteristicsUUID(index: Int, uuid: String) {
        when (index) {
            1 -> Constante1_CHARACTERISTICS_UUID = uuid
            2 -> Constante2_CHARACTERISTICS_UUID = uuid
            3 -> Constante3_CHARACTERISTICS_UUID = uuid
            4 -> Constante4_CHARACTERISTICS_UUID = uuid
        }
    }

    fun setFechaCharacteristicsUUID(index: Int, uuid: String) {
        when (index) {
            1 -> Fecha1_CHARACTERISTICS_UUID = uuid
            2 -> Fecha2_CHARACTERISTICS_UUID = uuid
            3 -> Fecha3_CHARACTERISTICS_UUID = uuid
            4 -> Fecha4_CHARACTERISTICS_UUID = uuid
        }
    }

    fun setAlertasCharacteristicsUUID(index: Int, uuid: String) {
        when (index) {
            1 -> Alertas1_CHARACTERISTICS_UUID = uuid
            2 -> Alertas2_CHARACTERISTICS_UUID = uuid
            3 -> Alertas3_CHARACTERISTICS_UUID = uuid
            4 -> Alertas4_CHARACTERISTICS_UUID = uuid
        }
    }

    fun setNotificacionDescriptorUUID(index: Int, uuid: String) {
        when (index) {
            1 -> Notificacion1_DESCRIPTOR_UUID = uuid
            2 -> Notificacion2_DESCRIPTOR_UUID = uuid
            3 -> Notificacion3_DESCRIPTOR_UUID = uuid
            4 -> Notificacion4_DESCRIPTOR_UUID = uuid
        }
    }

    fun setSensorId(index: Int, sensorId: String) {
        when (index) {
            1 -> SensorId1 = sensorId
            2 -> SensorId2 = sensorId
            3 -> SensorId3 = sensorId
            4 -> SensorId4 = sensorId
        }
    }

    fun setMac(index: Int, mac: String) {
        when (index) {
            1 -> mac1 = mac
            2 -> mac2 = mac
            3 -> mac3 = mac
            4 -> mac4 = mac
        }
    }

    fun setControlServiceUUID(uuid: String) {
        ControlService_SERVICE_UUID = uuid
    }

    fun setBateriaCharacteristicsUUID(uuid: String) {
        Bateria_CHARACTERISTICS_UUID = uuid
    }

    fun setSenialCharacteristicsUUID(uuid: String) {
        Senial_CHARACTERISTICS_UUID = uuid
    }

    fun setComandosCharacteristicsUUID(uuid: String) {
        Comandos_CHARACTERISTICS_UUID = uuid
    }

    fun setAlertasGCharacteristicsUUID(uuid: String) {
        AlertasG_CHARACTERISTICS_UUID = uuid
    }

    fun setNotificacionControlDescriptorUUID(uuid: String) {
        NotificacionControl_DESCRIPTOR_UUID = uuid
    }

    //////
    fun getSensorServiceUUID(index: Int): String {
        return when (index) {
            1 -> SENSOR1_SERVICE_UIID
            2 -> SENSOR2_SERVICE_UIID
            3 -> SENSOR3_SERVICE_UIID
            4 -> SENSOR4_SERVICE_UIID
            else -> ""
        }
    }

    fun getVolumenCharacteristicsUUID(index: Int): String {
        return when (index) {
            1 -> Volumen1_CHARACTERISTICS_UUID
            2 -> Volumen2_CHARACTERISTICS_UUID
            3 -> Volumen3_CHARACTERISTICS_UUID
            4 -> Volumen4_CHARACTERISTICS_UUID
            else -> ""
        }
    }

    fun getTemperaturaCharacteristicsUUID(index: Int): String {
        return when (index) {
            1 -> Temperatura1_CHARACTERISTICS_UUID
            2 -> Temperatura2_CHARACTERISTICS_UUID
            3 -> Temperatura3_CHARACTERISTICS_UUID
            4 -> Temperatura4_CHARACTERISTICS_UUID
            else -> ""
        }
    }

    fun getConstanteCharacteristicsUUID(index: Int): String {
        return when (index) {
            1 -> Constante1_CHARACTERISTICS_UUID
            2 -> Constante2_CHARACTERISTICS_UUID
            3 -> Constante3_CHARACTERISTICS_UUID
            4 -> Constante4_CHARACTERISTICS_UUID
            else -> ""
        }
    }

    fun getFechaCharacteristicsUUID(index: Int): String {
        return when (index) {
            1 -> Fecha1_CHARACTERISTICS_UUID
            2 -> Fecha2_CHARACTERISTICS_UUID
            3 -> Fecha3_CHARACTERISTICS_UUID
            4 -> Fecha4_CHARACTERISTICS_UUID
            else -> ""
        }
    }

    fun getAlertasCharacteristicsUUID(index: Int): String {
        return when (index) {
            1 -> Alertas1_CHARACTERISTICS_UUID
            2 -> Alertas2_CHARACTERISTICS_UUID
            3 -> Alertas3_CHARACTERISTICS_UUID
            4 -> Alertas4_CHARACTERISTICS_UUID
            else -> ""
        }
    }


}