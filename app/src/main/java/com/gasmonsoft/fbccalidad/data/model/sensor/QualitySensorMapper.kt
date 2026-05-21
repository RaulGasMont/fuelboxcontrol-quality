package com.gasmonsoft.fbccalidad.data.model.sensor

import com.gasmonsoft.fbccalidad.domain.model.QualitySensorRecord

fun LastQualitySensorData.toDomain(): QualitySensorRecord {
    return QualitySensorRecord(
        date = fechaRegistro,
        quality = calidad,
        temperature = temperatura
    )
}
