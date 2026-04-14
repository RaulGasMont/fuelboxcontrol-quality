package com.gasmonsoft.fuelboxcontrol.ui.sensor.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val dataStoreRepository: DataStoreRepository,
    private val sensorReceiveManager: SensorReceiveManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val ssidUuid = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb").toString()
    private val passwordUuid = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb").toString()
    private val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb").toString()

    private val globalDateUuid = "0fe0e4d2-724e-4e1a-bebe-79e29f621b15"
    private val globalAlertsUuid = "80c4c443-2128-4570-b0da-6b3dbced01a6"
    private val batteryUuid = "66169bab-d567-4388-b634-357ff0dac5f1"

    private val _uiState = MutableStateFlow(SensorUiState())
    val uiState: StateFlow<SensorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SensorUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SensorUiEvent> = _events.asSharedFlow()

    val connectionState: StateFlow<ConnectionState> =
        uiState.map { it.connectionState }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ConnectionState.Uninitialized
            )

    val shouldReconnect: StateFlow<Boolean> =
        uiState.map { it.shouldReconnect }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true
            )

    private var subscriptionJob: Job? = null
    private var periodicWriteJob: Job? = null

    val sensorInfoState = sensorReceiveManager.sensorData

    fun initializeConnection() {
        if (uiState.value.connectionState == ConnectionState.Connected ||
            uiState.value.connectionState == ConnectionState.CurrentlyInitializing) {
            return
        }
        loadSavedMacAddress()
        clearReadings()
        ensureSubscription()
        sensorReceiveManager.startReceiving()
    }

    fun enableAutoReconnect() {
        _uiState.update { it.copy(shouldReconnect = true) }
    }

    fun disableAutoReconnect() {
        _uiState.update { it.copy(shouldReconnect = false) }
    }

    fun disconnect() {
        viewModelScope.launch {
            stopPeriodicWriteTask()
            subscriptionJob?.cancel()

            _uiState.update {
                it.copy(
                    shouldReconnect = false,
                    connectionState = ConnectionState.Disconnected,
                    initializingMessage = null
                )
            }
            dataStoreRepository.clearTank()
            sensorReceiveManager.disconnect()
        }
    }

    fun reconnect() {
        _uiState.update {
            it.copy(
                shouldReconnect = true,
                errorMessage = null
            )
        }
        ensureSubscription()
        sensorReceiveManager.reconnect()
    }

    fun clearValores() {
        clearReadings()
    }

    fun updateHotspotConfigurationStatus(isConfigured: Boolean) {
        _uiState.update { it.copy(isHotspotConfigured = isConfigured) }
    }

    fun updateHotspotConnectionStatus(isConnected: Boolean) {
        _uiState.update { it.copy(isConnectedToHotspot = isConnected) }
    }

    fun updateHotspotAvailability(isAvailable: Boolean) {
        _uiState.update { it.copy(isHotspotAvailable = isAvailable) }
    }

    fun updateDiscoveredServices(services: String) {
        _uiState.update { it.copy(discoveredServices = services) }
    }

    fun updateAutoconsumo(enabled: Boolean) {
        _uiState.update { it.copy(isAutoconsumo = enabled) }
    }

    fun writeCommand(value: String, option: Int) {
        sensorReceiveManager.writeInitialValuese(value, option)
    }

    fun onwrite(defaultSensorResponse: String) = writeCommand(defaultSensorResponse, 1)
    fun onwriteEemo(defaultSensorResponse: String) = writeCommand(defaultSensorResponse, 32)
    fun onwriteElmo(defaultSensorResponse: String) = writeCommand(defaultSensorResponse, 33)
    fun onwritesace(defaultSensorResponse: String) = writeCommand(defaultSensorResponse, 34)
    fun onWriteEinc(defaultSensorResponse: String) = writeCommand(defaultSensorResponse, 6)
    fun onWriteRTC(defaultSensorResponse: String) = writeCommand(defaultSensorResponse, 7)

    fun writesDataHostPost(ssid: String, password: String, isWifiEnabled: Boolean) {
        sensorReceiveManager.writesDataHostPost(ssid, password, isWifiEnabled)
    }

    fun startPeriodicWriteTask() {
        if (periodicWriteJob?.isActive == true) return

        periodicWriteJob = viewModelScope.launch {
            while (true) {
                sensorReceiveManager.writeInitialValuese("", 2)
                delay(30_000)
            }
        }
    }

    fun stopPeriodicWriteTask() {
        periodicWriteJob?.cancel()
        periodicWriteJob = null
    }

    fun scheduleSingleDelayedWrite() {
        viewModelScope.launch {
            delay(70_000)
            sensorReceiveManager.writeInitialValuese("", 3)
        }
    }

    suspend fun configureHotspot(ssid: String, password: String, isEnabled: Boolean) {
        try {
            _events.tryEmit(
                SensorUiEvent.ShowToast(
                    "Espere un momento hasta que se le indique conectar a la red."
                )
            )

            _uiState.update {
                it.copy(
                    hotspotConfigurationMessage = "Configurando red...",
                    isHotspotConfigured = false,
                    isSendingCommand = true
                )
            }

            writesDataHostPost(ssid, password, isEnabled)

            delay(20_000)
            onwrite("1")
            delay(1_000)

            _uiState.update {
                it.copy(
                    hotspotConfigurationMessage = "Hotspot configurado correctamente",
                    isHotspotConfigured = true,
                    isSendingCommand = false
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    hotspotConfigurationMessage = "Error al configurar red: ${e.message}",
                    isHotspotConfigured = false,
                    isSendingCommand = false
                )
            }
            _events.tryEmit(
                SensorUiEvent.ShowError("No fue posible configurar la red.")
            )
        }
    }

    @SuppressLint("MissingPermission", "ServiceCast")
    fun connectToHotspot(
        ssid: String,
        password: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    connectToHotspotAndroidQAndAbove(ssid, password)
                } else {
                    connectToHotspotLegacy(ssid, password)
                }

                _uiState.update { it.copy(isConnectedToHotspot = result) }
                onResult(result)
            } catch (_: Exception) {
                _uiState.update { it.copy(isConnectedToHotspot = false) }
                onResult(false)
            }
        }
    }

    private fun ensureSubscription() {
        if (subscriptionJob?.isActive == true) return

        subscriptionJob = viewModelScope.launch {
            sensorReceiveManager.data.collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update {
                            it.copy(
                                connectionState = ConnectionState.CurrentlyInitializing,
                                initializingMessage = result.message,
                                errorMessage = null
                            )
                        }
                    }

                    is Resource.Error -> {
                        stopPeriodicWriteTask()
                        _uiState.update {
                            it.copy(
                                connectionState = ConnectionState.Uninitialized,
                                initializingMessage = null,
                                errorMessage = result.errorMessage
                            )
                        }
                    }

                    is Resource.Success -> {
                        val sensorId = result.data.SensorId
                        val rawValue = result.data.Volumen
                        val newConnectionState = result.data.connectionState

                        _uiState.update { current ->
                            reduceIncomingPacket(
                                current = current,
                                sensorId = sensorId,
                                rawValue = rawValue,
                                connectionState = newConnectionState
                            )
                        }

                        when (newConnectionState) {
                            ConnectionState.Connected -> startPeriodicWriteTask()
                            ConnectionState.Disconnected,
                            ConnectionState.Uninitialized -> stopPeriodicWriteTask()

                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun reduceIncomingPacket(
        current: SensorUiState,
        sensorId: String,
        rawValue: String,
        connectionState: ConnectionState
    ): SensorUiState {
        val convertedValue = formatRawOrConverted(rawValue)

        val updatedState = when (sensorId) {
            NetworkConfig.Volumen1_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor1 = current.sensor1.copy(
                        volumen = "Volumen 1: $convertedValue"
                    )
                )
            }

            NetworkConfig.Temperatura1_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor1 = current.sensor1.copy(
                        temperatura = "Calidad 1: $convertedValue"
                    )
                )
            }

            NetworkConfig.Constante1_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor1 = current.sensor1.copy(
                        constante = "Temperatura 1: $convertedValue"
                    )
                )
            }

            globalDateUuid -> {
                current.copy(
                    sensor1 = current.sensor1.copy(
                        fecha = "Fecha global: $convertedValue"
                    )
                )
            }

            NetworkConfig.Alertas1_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor1 = current.sensor1.copy(
                        alerta = "Alertas 1: $convertedValue"
                    )
                )
            }

            NetworkConfig.Volumen2_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor2 = current.sensor2.copy(
                        volumen = "Volumen 2: $convertedValue"
                    )
                )
            }

            NetworkConfig.Temperatura2_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor2 = current.sensor2.copy(
                        temperatura = "Calidad 2: $convertedValue"
                    )
                )
            }

            NetworkConfig.Constante2_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor2 = current.sensor2.copy(
                        constante = "Temperatura 2: $convertedValue"
                    )
                )
            }

            NetworkConfig.Alertas2_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor2 = current.sensor2.copy(
                        alerta = "Alertas 2: $convertedValue"
                    )
                )
            }

            NetworkConfig.Volumen3_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor3 = current.sensor3.copy(
                        volumen = "Volumen 3: $convertedValue"
                    )
                )
            }

            NetworkConfig.Temperatura3_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor3 = current.sensor3.copy(
                        temperatura = "Calidad 3: $convertedValue"
                    )
                )
            }

            NetworkConfig.Constante3_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor3 = current.sensor3.copy(
                        constante = "Temperatura 3: $convertedValue"
                    )
                )
            }

            NetworkConfig.Fecha3_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor3 = current.sensor3.copy(
                        fecha = "Fecha 3: $convertedValue"
                    )
                )
            }

            NetworkConfig.Alertas3_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor3 = current.sensor3.copy(
                        alerta = "Alertas 3: $convertedValue"
                    )
                )
            }

            NetworkConfig.Volumen4_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor4 = current.sensor4.copy(
                        volumen = "Volumen 4: $convertedValue"
                    )
                )
            }

            NetworkConfig.Temperatura4_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor4 = current.sensor4.copy(
                        temperatura = "Calidad 4: $convertedValue"
                    )
                )
            }

            NetworkConfig.Constante4_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor4 = current.sensor4.copy(
                        constante = "Temperatura 4: $convertedValue"
                    )
                )
            }

            NetworkConfig.Fecha4_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor4 = current.sensor4.copy(
                        fecha = "Fecha 4: $convertedValue"
                    )
                )
            }

            NetworkConfig.Alertas4_CHARACTERISTICS_UUID -> {
                current.copy(
                    sensor4 = current.sensor4.copy(
                        alerta = "Alertas 4: $convertedValue"
                    )
                )
            }

            ssidUuid,
            passwordUuid,
            wifiEnabledUuid -> {
                current.copy(
                    sendingState = rawValue
                )
            }

            globalAlertsUuid -> {
                current.copy(
                    sensorMessage = decodeGlobalAlert(rawValue)
                )
            }

            batteryUuid -> {
                current.copy(
                    bateria = "Comando: $rawValue"
                )
            }

            else -> current
        }

        return updatedState.copy(
            connectionState = connectionState,
            initializingMessage = null,
            errorMessage = null
        )
    }

    private fun clearReadings() {
        stopPeriodicWriteTask()
        _uiState.update { current ->
            current.copy(
                initializingMessage = null,
                errorMessage = null,
                sensor1 = SensorCardUiState(),
                sensor2 = SensorCardUiState(),
                sensor3 = SensorCardUiState(),
                sensor4 = SensorCardUiState(),
                bateria = "",
                sensorMessage = ""
            )
        }
    }

    private fun loadSavedMacAddress() {
        val sharedPreferences =
            context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
        val savedMacAddress = sharedPreferences.getString("last_mac", "") // Corregido: "last_mac"

        if (!savedMacAddress.isNullOrEmpty()) {
            NetworkConfig.nombreconfiguracion = savedMacAddress
            NetworkConfig.configuracion = "mac"
        }
    }

    private fun formatRawOrConverted(rawValue: String): String {
        val converted = ordenarYConvertir(rawValue)
        return converted?.let { value ->
            String.format(Locale.getDefault(), "%.2f", value)
        } ?: rawValue
    }

    fun ordenarYConvertir(volumen: String): Float? {
        return try {
            if (volumen.length % 2 != 0) return null

            val volumenOrdenado = StringBuilder()
            for (i in volumen.indices step 2) {
                volumenOrdenado.insert(0, volumen.substring(i, i + 2))
            }

            val intBits = volumenOrdenado.toString().toLong(16).toInt()
            Float.fromBits(intBits)
        } catch (_: NumberFormatException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeGlobalAlert(hexValue: String): String {
        return try {
            if (hexValue.length < 8) return "Alerta global: $hexValue"

            val alertByte = hexValue.substring(6, 8).toInt(16)

            val sensorActivo =
                if ((alertByte and 0b0000_0001) != 0) "Sensor activo" else "Sensor inactivo"
            val funcionando =
                if ((alertByte and 0b0000_0100) != 0) "No está funcionando" else "Funcionando"
            val tanque5 =
                if ((alertByte and 0b0000_1000) != 0) "Tanque menor al 5%" else "Tanque mayor al 5%"
            val tanque95 =
                if ((alertByte and 0b0001_0000) != 0) "Tanque mayor al 95%" else "Tanque no mayor al 95%"
            val cambioTemperatura =
                if ((alertByte and 0b0010_0000) != 0) "Cambio de temperatura" else "Sin cambio de temperatura"
            val cambioCombustible =
                if ((alertByte and 0b0100_0000) != 0) "Cambio de combustible" else "Sin cambio de combustible"
            val cambioNivel =
                if ((alertByte and 0b1000_0000) != 0) "Cambio de nivel" else "Sin cambio de nivel"

            buildString {
                appendLine("Alerta global")
                appendLine("- $sensorActivo")
                appendLine("- $funcionando")
                appendLine("- $tanque5")
                appendLine("- $tanque95")
                appendLine("- $cambioTemperatura")
                appendLine("- $cambioCombustible")
                append("- $cambioNivel")
            }
        } catch (_: Exception) {
            "Alerta global sin decodificar: $hexValue"
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    @SuppressLint("MissingPermission")
    private suspend fun connectToHotspotAndroidQAndAbove(
        ssid: String,
        password: String
    ): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val networkAvailable = CompletableDeferred<Boolean>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.bindProcessToNetwork(network)
                if (!networkAvailable.isCompleted) {
                    networkAvailable.complete(true)
                }
            }

            override fun onUnavailable() {
                if (!networkAvailable.isCompleted) {
                    networkAvailable.complete(false)
                }
            }

            override fun onLost(network: Network) {
                if (!networkAvailable.isCompleted) {
                    networkAvailable.complete(false)
                }
            }
        }

        return try {
            connectivityManager.requestNetwork(request, callback)
            withTimeoutOrNull(15_000L) { networkAvailable.await() } ?: false
        } finally {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun connectToHotspotLegacy(
        ssid: String,
        password: String
    ): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        }

        val netId = wifiManager.addNetwork(config)
        if (netId == -1) return false

        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()

        delay(3_000)
        return true
    }

//    override fun onCleared() {
//        stopPeriodicWriteTask()
//        subscriptionJob?.cancel()
//        sensorReceiveManager.closeConnection()
//        super.onCleared()
//    }
}