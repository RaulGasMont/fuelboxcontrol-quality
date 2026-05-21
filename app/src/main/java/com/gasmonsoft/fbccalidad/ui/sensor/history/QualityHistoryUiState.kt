package com.gasmonsoft.fbccalidad.ui.sensor.history

import com.gasmonsoft.fbccalidad.domain.model.QualitySensorRecord
import com.gasmonsoft.fbccalidad.utils.LoadState

data class QualityHistoryUiState(
    val records: List<QualitySensorRecord> = emptyList(),
    val loadState: LoadState = LoadState.Idle,
    val idCajaCalidad: Int = -1
)
