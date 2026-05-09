package com.gasmonsoft.fbccalidad.ui.detector.viewmodel

import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.model.sensor.SensorCalidadData
import com.gasmonsoft.fbccalidad.data.repository.api.FuelSoftwareControlRepository
import com.gasmonsoft.fbccalidad.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fbccalidad.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fbccalidad.data.repository.usb.UsbRepository
import com.gasmonsoft.fbccalidad.domain.detector.DetectorUseCase
import com.gasmonsoft.fbccalidad.domain.detector.MatterUnity
import com.gasmonsoft.fbccalidad.domain.usb.UsbConnectionMonitor
import com.gasmonsoft.fbccalidad.domain.usb.UsbDeviceState
import com.gasmonsoft.fbccalidad.domain.usb.UsbPermissionManager
import com.gasmonsoft.fbccalidad.domain.usb.UsbPermissionResult
import com.gasmonsoft.fbccalidad.ui.detector.screen.FuelType
import com.gasmonsoft.fbccalidad.utils.ProcessingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectorViewModel @Inject constructor(
    sensorReceiveManager: SensorReceiveManager,
    private val detectorUseCase: DetectorUseCase,
    private val fscApiRepository: FuelSoftwareControlRepository,
    dataStoreRepository: DataStoreRepository,
    private val usbRepository: UsbRepository,
    private val usbPermissionManager: UsbPermissionManager,
    private val usbConnectionMonitor: UsbConnectionMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetectorUiState())
    val uiState: StateFlow<DetectorUiState> = _uiState.asStateFlow()

    val selectedTankState = dataStoreRepository.selectedTank.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val selectedBox = dataStoreRepository.selectedCaja.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private var usbReadingJob: Job? = null

    private val sensorData = sensorReceiveManager.sensorData
    private val usbData = usbRepository.sensorData

    init {
        getFuelTypes()
        observeTankSelection()
        observeBoxSelection()
        observeUsbConnection()
        observeUsbPermission()
        usbConnectionMonitor.startMonitoring()
    }

    private fun getFuelTypes() {
        viewModelScope.launch {
            val result = fscApiRepository.getMatters()
//        val newMap = _uiState.value.matterUnities.toMutableMap()
//        val fuelRanges = FuelType.entries.forEach { type ->
//            newMap[type]
//        }
//        _uiState.update { currentUiState ->
//            currentUiState.copy(
//                matterUnities = newMap
//            )
//        }
        }
    }

    private fun observeUsbConnection() {
        viewModelScope.launch {
            usbConnectionMonitor.deviceState.collect { usbState ->
                Log.d("DetectorVM", "USB State changed: $usbState")
                when (usbState) {
                    is UsbDeviceState.Attached -> {
                        val device = usbState.device
                        val hasPermission = usbPermissionManager.hasPermission(device)
                        Log.d(
                            "DetectorVM",
                            "USB Device detected: ${device.deviceName}. HasPermission=$hasPermission"
                        )
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                otgTransferState = if (hasPermission) {
                                    TransferState.DeviceReady(device)
                                } else {
                                    TransferState.DeviceDetected(device)
                                }
                            )
                        }
                    }

                    is UsbDeviceState.Disconnected -> {
                        Log.d("DetectorVM", "USB Device disconnected.")
                        usbRepository.disconnect()
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                otgTransferState = TransferState.Idle
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }

    fun observeBoxSelection() {
        viewModelScope.launch {
            selectedBox.collect {
                if (it != null) {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            idCaja = it
                        )
                    }
                }
            }
        }
    }

    fun observeTankSelection() {
        viewModelScope.launch {
            selectedTankState.collect {
                if (it != null) {
                    _uiState.update { currentUiState ->
                        currentUiState.copy(
                            tankId = it.tankId,
                            tankType = it.tankType,
                            tankName = it.tankName.ifEmpty { "Tanque ${it.tankId}" }
                        )
                    }
                }
            }
        }
    }

    fun refreshDetection() {
        usbConnectionMonitor.startMonitoring()
    }

    fun analyzeData(channel: DetectorChannelType) {
        updateDetectionEvent(ProcessingEvent.Loading)
        viewModelScope.launch {
            val result = when (channel) {
                DetectorChannelType.BLE -> {
                    detectorUseCase(sensorData, matterList)
                }

                DetectorChannelType.USB -> {
                    val isReady = prepareUsbReading()
                    if (isReady) {
                        detectorUseCase(usbData, matterList)
                    } else {
                        MatterUnity(FuelType.DESCONOCIDO, null, null)
                    }
                }
            }

            fscApiRepository.sendSensorCalidadData(
                SensorCalidadData(
                    idCajaCalidad = _uiState.value.idCaja,
                    idTipoContenedor = _uiState.value.tankType,
                    calidad = result.value?.toDouble() ?: 0.0,
                    temperatura = result.temperature?.toDouble() ?: 0.0
                )
            )

//            if (result.type != FuelType.DIESEL) {
////                fscApiRepository.sendFuelAlert(
//////                    body = FuelAlert(
//////                        fechaRegistro = TODO(),
//////                        tipo = result,
//////                    )
////                )
//            }

            _uiState.update { currentUiState ->
                currentUiState.copy(
                    fuelType = result.type,
                    valueDetection = result.value ?: 0.0f
                )
            }

            updateDetectionEvent(ProcessingEvent.Success)

            if (usbReadingJob != null) {
                usbReadingJob?.cancel()
                usbReadingJob = null
            }
        }
    }

    fun updateDetectionEvent(status: ProcessingEvent) {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                detectionEvent = status
            )
        }
    }

    private suspend fun prepareUsbReading(): Boolean {
        return when (val otgState = _uiState.value.otgTransferState) {
            is TransferState.DeviceReady -> {
                val connected = usbRepository.connect(
                    device = otgState.device
                )
                if (connected) {
                    usbRepository.startReading()
                    true
                } else false
            }

            is TransferState.DeviceDetected -> {
                val connected = usbRepository.connect(
                    device = otgState.device,
                )
                if (connected) {
                    usbRepository.startReading()
                    true
                } else {
                    onPermissionRequest(otgState.device)
                    false
                }
            }

            else -> false
        }
    }

    private fun observeUsbPermission() {
        viewModelScope.launch {
            usbPermissionManager.permissionState.collect { result ->
                Log.d("USB", result.toString())
                when (result) {
                    is UsbPermissionResult.Granted -> {
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                otgTransferState = TransferState.DeviceReady(result.device)
                            )
                        }
                    }

                    is UsbPermissionResult.Denied -> {
                        _uiState.update { currentUiState ->
                            currentUiState.copy(
                                otgTransferState = TransferState.Error("Permiso USB denegado")
                            )
                        }
                    }

                }
            }
        }
    }

    fun onPermissionRequest(device: UsbDevice) {
        _uiState.update { currentUiState ->
            currentUiState.copy(
                otgTransferState = TransferState.RequestingPermission
            )
        }
        usbPermissionManager.requestPermission(device)
    }

    override fun onCleared() {
        super.onCleared()
        // No detenemos el monitoreo aquí ya que es un Singleton manejado por la App/Activity
        usbRepository.disconnect()
    }
}
