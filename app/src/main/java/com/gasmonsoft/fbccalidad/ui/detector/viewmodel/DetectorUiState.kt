package com.gasmonsoft.fbccalidad.ui.detector.viewmodel

import com.gasmonsoft.fbccalidad.ui.detector.screen.FuelType
import com.gasmonsoft.fbccalidad.utils.ProcessingEvent

data class DetectorUiState(
    val tankType: Boolean = false,
    val tankId: Int = -1,
    val tankName: String = "",
    val idCaja: Int = -1,
    val fuelType: FuelType = FuelType.DESCONOCIDO,
    val valueDetection: Float = 0.0f,
    val detectionEvent: ProcessingEvent = ProcessingEvent.Idle
)