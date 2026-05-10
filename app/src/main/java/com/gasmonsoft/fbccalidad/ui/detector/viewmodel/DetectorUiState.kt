package com.gasmonsoft.fbccalidad.ui.detector.viewmodel

import com.gasmonsoft.fbccalidad.domain.model.QualityRange
import com.gasmonsoft.fbccalidad.utils.ProcessingEvent

data class DetectorUiState(
    val tankType: Boolean = false,
    val tankId: Int = -1,
    val tankName: String = "",
    val idCaja: Int = -1,
    val fuelType: QualityRange = QualityRange.DESCONOCIDO,
    val valueDetection: Float = 0.0f,
    val detectionEvent: ProcessingEvent = ProcessingEvent.Idle,
    val channel: DetectorChannelType = DetectorChannelType.BLE,
    val otgTransferState: TransferState = TransferState.Idle,
    val loadScreen: ProcessingEvent = ProcessingEvent.Loading,
    val fuelTypes: List<QualityRange> = emptyList()
)

data class MatterRange(
    val min: Double,
    val max: Double
)

enum class DetectorChannelType {
    BLE, USB
}