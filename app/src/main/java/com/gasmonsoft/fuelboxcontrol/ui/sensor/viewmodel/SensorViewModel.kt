package com.gasmonsoft.fuelboxcontrol.ui.sensor.viewmodel

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.repository.ble.SensorReceiveManager
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _shouldReconnect = MutableStateFlow(true)
    val shouldReconnect: StateFlow<Boolean> = _shouldReconnect.asStateFlow()

    fun disableAutoReconnect() {
        _shouldReconnect.value = false
    }


    fun enableAutoReconnect() {
        _shouldReconnect.value = true
    }


    var initializingMessage by mutableStateOf<String?>(null)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var sensoruno by mutableStateOf("")
        private set

    private val _connectionState: MutableStateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState.Uninitialized)
    val connectionState = _connectionState.asStateFlow()

    val _Message = MutableLiveData<String>()
    val Message: LiveData<String> = _Message

    private val _volumen1 = MutableLiveData<String>()
    val volumen1: LiveData<String> = _volumen1
    private val _temperatura1 = MutableLiveData<String>()
    val temperatura1: LiveData<String> = _temperatura1
    private val _constante1 = MutableLiveData<String>()
    val constante1: LiveData<String> = _constante1
    private val _fecha1 = MutableLiveData<String>()
    val fecha1: LiveData<String> = _fecha1
    private val _alerta1 = MutableLiveData<String>()
    val alerta1: LiveData<String> = _alerta1

    private val _volumen2 = MutableLiveData<String>()
    val volumen2: LiveData<String> = _volumen2
    private val _temperatura2 = MutableLiveData<String>()
    val temperatura2: LiveData<String> = _temperatura2
    private val _constante2 = MutableLiveData<String>()
    val constante2: LiveData<String> = _constante2
    private val _fecha2 = MutableLiveData<String>()
    val fecha2: LiveData<String> = _fecha2
    private val _alerta2 = MutableLiveData<String>()
    val alerta2: LiveData<String> = _alerta2

    private val _volumen3 = MutableLiveData<String>()
    val volumen3: LiveData<String> = _volumen3
    private val _temperatura3 = MutableLiveData<String>()
    val temperatura3: LiveData<String> = _temperatura3
    private val _constante3 = MutableLiveData<String>()
    val constante3: LiveData<String> = _constante3
    private val _fecha3 = MutableLiveData<String>()
    val fecha3: LiveData<String> = _fecha3
    private val _alerta3 = MutableLiveData<String>()
    val alerta3: LiveData<String> = _alerta3

    private val _volumen4 = MutableLiveData<String>()
    val volumen4: LiveData<String> = _volumen4
    private val _temperatura4 = MutableLiveData<String>()
    val temperatura4: LiveData<String> = _temperatura4
    private val _constante4 = MutableLiveData<String>()
    val constante4: LiveData<String> = _constante4
    private val _fecha4 = MutableLiveData<String>()
    val fecha4: LiveData<String> = _fecha4
    private val _alerta4 = MutableLiveData<String>()
    val alerta4: LiveData<String> = _alerta4

    private val _senial = MutableLiveData<String>()
    val senial: LiveData<String> = _senial

    private val _bateria = MutableLiveData<String>()
    val bateria: LiveData<String> = _bateria

    private val _textValue = MutableLiveData<String>()
    val textValue: LiveData<String> = _textValue

    private var alertDialog: AlertDialog? = null


    private val timer = Timer()


    //wifi

    private val _connectionStates = MutableStateFlow("Desconectado")
    val connectionStates: StateFlow<String> = _connectionStates

    private val _sendingState = MutableStateFlow("Esperando hostpost")
    val sendingState: StateFlow<String> = _sendingState

    private val _discoveredServices = MutableStateFlow("")
    val discoveredServices: StateFlow<String> = _discoveredServices

    val sensorInfoState = sensorReceiveManager.sensorData

    var isAutoconsumo by mutableStateOf(false)
        private set

    init {
        scheduleWriteInitialValuesTask()
    }

    private fun scheduleWriteInitialValuesTask() {
        timer.schedule(object : TimerTask() {
            override fun run() {

                writeInitialValuese("", 2)

            }
        }, 0, 30000)
    }

    private fun scheduleWriteInitialValuesTask2() {
        val task = object : TimerTask() {
            override fun run() {
                writeInitialValuese("", 3)

                cancel()
            }
        }

        timer.schedule(task, 70000) // 34000 milisegundos = 34 segundos
    }


    fun onCleareds() {
        super.onCleared()
        timer.cancel()
        sensorReceiveManager.closeConnection()
        _Message.value = ""
        _bateria.value = ""
    }

//    @SuppressLint("MissingPermission")
//    suspend fun getLastKnownLocation(): Location? {
//        return try {
//            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
//            suspendCoroutine { continuation ->
//                var isResumed = false
//
//                val locationRequest = LocationRequest.create()
//                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                    .setInterval(10000)
//                    .setFastestInterval(5000)
//
//                val callback = object : LocationCallback() {
//                    override fun onLocationResult(p0: LocationResult) {
//                        if (!isResumed) {
//                            p0.lastLocation?.let { location ->
//                                continuation.resume(location)
//                            } ?: run {
//                                continuation.resume(null)
//                            }
//                            isResumed = true
//                            fusedLocationClient.removeLocationUpdates(this)
//                        }
//                    }
//                }
//
//                fusedLocationClient.requestLocationUpdates(
//                    locationRequest,
//                    callback,
//                    Looper.getMainLooper()
//                )
//            }
//        } catch (e: Exception) {
//
//            null
//        }
//    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onSendRead(context: Context, uri: Uri, date: String, idcaja: String) {
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun subscribeToChanges() {
        _Message.value = ""
        var latitude: Double
        var longitude: Double
        viewModelScope.launch {

            val ssidUuid = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb")
            val passwordUuid = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
            val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
//
            sensorReceiveManager.data.collect { result ->

//                val location = getLastKnownLocation()
//
//                if (location != null) {
//                    latitude = location.latitude
//                    longitude = location.longitude
//                } else {
//                    latitude = 0.0
//                    longitude = 0.0
//                }

                _connectionState.update {
                    when (result) {
                        is Resource.Success -> {
                            with(result.data.SensorId) {
                                when (result.data.SensorId) {
                                    NetworkConfig.Volumen1_CHARACTERISTICS_UUID -> {
                                        _volumen1.value =
                                            "Volúmen1 ${NetworkConfig.Volumen1_CHARACTERISTICS_UUID}: " + (ordenarYConvertir(
                                                result.data.Volumen
                                            ) ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Temperatura1_CHARACTERISTICS_UUID -> {

                                        _temperatura1.value =
                                            "Calidad1 ${NetworkConfig.Temperatura1_CHARACTERISTICS_UUID}: " + (ordenarYConvertir(
                                                result.data.Volumen
                                            ) ?: result.data.Volumen)
                                    }

                                    NetworkConfig.Constante1_CHARACTERISTICS_UUID -> {

                                        _constante1.value =
                                            "Temperatura1 ${NetworkConfig.Constante1_CHARACTERISTICS_UUID}: " + (ordenarYConvertir(
                                                result.data.Volumen
                                            ) ?: result.data.Volumen)
                                    }

                                    "0fe0e4d2-724e-4e1a-bebe-79e29f621b15" -> {
                                        _fecha1.value =
                                            "Fecha Global: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Alertas1_CHARACTERISTICS_UUID -> {

                                        _alerta1.value =
                                            "Alertas1: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }


                                    NetworkConfig.Volumen2_CHARACTERISTICS_UUID -> {

                                        _volumen2.value =
                                            "Volúmen2: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }

                                    NetworkConfig.Temperatura2_CHARACTERISTICS_UUID -> {
                                        _temperatura2.value =
                                            "Calidad2: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Constante2_CHARACTERISTICS_UUID -> {

                                        _constante2.value =
                                            "Temperatura2: " + ordenarYConvertir(result.data.Volumen)
                                    }


                                    ssidUuid.toString() -> {

                                        _sendingState.value = result.data.Volumen
                                    }

                                    passwordUuid.toString() -> {

                                        _sendingState.value = result.data.Volumen
                                    }

                                    wifiEnabledUuid.toString() -> {

                                        _sendingState.value = result.data.Volumen
                                    }

                                    "80c4c443-2128-4570-b0da-6b3dbced01a6" -> {
                                        _alerta2.value =
                                            "Alerta Global Sin convertir" + result.data.Volumen
                                        val hexValue = result.data.Volumen
                                        val binaryValues = kotlin.text.StringBuilder()


                                        for (character in hexValue) {
                                            val binaryValue =
                                                character.toString().toInt(16).toString(2)
                                                    .padStart(4, '0')
                                            binaryValues.append("$character binario: $binaryValue\n")
                                        }

//                                        val hexValueSubset = hexValue.substring(4, 6)
//                                        val binaryValueSubset = hexValueSubset.toInt(16).toString(2)
//                                        val bit4_1Result =
//                                            if (binaryValueSubset[0] == '1') "Sensor activo" else "Sensor inactivo"
//                                        val bit4_3Result =
//                                            if (binaryValueSubset[2] == '1') "No está Funcionando" else "Funcionando"
//                                        val bit4_4Result =
//                                            if (binaryValueSubset[3] == '1') "Tanque menor del 5 porciento" else "Tanque mayor al 5% porciento"
//                                        val bit4_5Result =
//                                            if (binaryValueSubset[4] == '1') "Tanque a mayor de 95%" else "El tanque no está a mayor de 95%"
//                                        val bit5_2Result =
//                                            if (binaryValueSubset[5] == '1') "Cambio de temperatura si" else "Cambio de temperatura no"
//                                        val bit5_3Result =
//                                            if (binaryValueSubset[6] == '1') "Cambio de combustible si" else "Calidad de combustible no"
//                                        val bit5_4Result =
//                                            if (binaryValueSubset[7] == '1') "Cambio de nivel si" else "Cambio de nivel no"
//
//
//                                        val finalResults =
//                                            "Valor binario Sensor1:$binaryValueSubset \n" +
//                                                    ", Primer Posición: $bit4_1Result\n" +
//                                                    ",  Tercera Posición Funcionado: $bit4_3Result\n" +
//                                                    ",  Cuarta Posicion: $bit4_4Result\n" +
//                                                    ", - Quinta Posicion Tanque 95%: $bit4_5Result\n" +
//                                                    " Sexta Posicion  Temperatura: $bit5_2Result\n" +
//                                                    ", Séptima Combustible: $bit5_3Result\n" +
//                                                    ",  Octava Nivel: $bit5_4Result\n\n\n\n"

                                        val hexValueSubsets = hexValue.substring(6, 8)

                                        val binaryValueSubsets = hexValueSubsets.toInt(16)
                                            .toString(2)

                                        val bit4_1Results = if (binaryValueSubsets.length == 1)
                                            if (binaryValueSubsets[0] == '1') "Sensor activo" else "Sensor inactivo"
                                        else "Campo no contemplado"

                                        val bit4_3Results = if (binaryValueSubsets.length == 3) {
                                            if (binaryValueSubsets[2] == '1') "No está Funcionando" else "Funcionando"
                                        } else "Campo no contemplado"

                                        val bit4_4Results = if (binaryValueSubsets.length == 4)
                                            if (binaryValueSubsets[3] == '1') "Tanque menor del 5 porciento" else "Tanque mayor al 5% porciento"
                                        else "Campo no contemplado"

                                        val bit4_5Results =
                                            if (binaryValueSubsets.length == 5) if (binaryValueSubsets[4] == '1')
                                                "Tanque a mayor de 95%" else "El tanque no está a mayor de 95%"
                                            else "Campo no contemplado"

                                        val bit5_2Results = if (binaryValueSubsets.length == 6)
                                            if (binaryValueSubsets[5] == '1') "Cambio de temperatura si" else "Cambio de temperatura no"
                                        else "Campo no contemplado"
                                        val bit5_3Results = if (binaryValueSubsets.length == 7)
                                            if (binaryValueSubsets[6] == '1') "Cambio de combustible si" else "Calidad de combustible no"
                                        else "Campo no contemplado"

                                        val bit5_4Results = if (binaryValueSubsets.length == 8)
                                            if (binaryValueSubsets[7] == '1') "Cambio de nivel si" else "Cambio de nivel no"
                                        else "Campo no contemplado"

                                        val finalResultss =
                                            "Sensor 2  \n- Sensor: $bit4_1Results\n  Funcionando: $bit4_3Results\n  Tanque: $bit4_4Results\nNovena Posicion\n - Tanque 95%: $bit4_5Results\n" +
                                                    "   Temperatura: $bit5_2Results\n  Combustible: $bit5_3Results\n  Nivel: $bit5_4Results\n"

//                                        val message =
//                                            "Alertas Globales: sin convertir ${result.data.Volumen}" +
//                                                    ",\n hexadecimal: $hexValue\n$binaryValues\n$finalResults\n" +
//                                                    "$finalResultss"
                                        _fecha2.value = ""
                                    }

                                    NetworkConfig.Alertas2_CHARACTERISTICS_UUID -> {

                                        _alerta2.value =
                                            "Alertas2: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }

                                    NetworkConfig.Volumen3_CHARACTERISTICS_UUID -> {
                                        _volumen3.value = "Volúmen3: " + result.data.Volumen

                                    }

                                    NetworkConfig.Temperatura3_CHARACTERISTICS_UUID -> {

                                        _temperatura3.value =
                                            "CALIDAD3: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }

                                    NetworkConfig.Constante3_CHARACTERISTICS_UUID -> {
                                        _constante3.value =
                                            "Temperatura3: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Fecha3_CHARACTERISTICS_UUID -> {
                                        _fecha3.value =
                                            "Fecha3: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Alertas3_CHARACTERISTICS_UUID -> {
                                        _alerta3.value =
                                            "Alertas3: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }


                                    NetworkConfig.Volumen4_CHARACTERISTICS_UUID -> {
                                        _volumen4.value =
                                            "Volúmen4: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Temperatura4_CHARACTERISTICS_UUID -> {
                                        _temperatura4.value =
                                            "Calidad: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Constante4_CHARACTERISTICS_UUID -> {

                                        _constante4.value =
                                            "Temperatura4: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }

                                    NetworkConfig.Fecha4_CHARACTERISTICS_UUID -> {
                                        _fecha4.value =
                                            "Fecha4: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)

                                    }

                                    NetworkConfig.Alertas4_CHARACTERISTICS_UUID -> {
                                        _alerta4.value =
                                            "Alertas4: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }

                                    NetworkConfig.Alertas4_CHARACTERISTICS_UUID -> {
                                        _alerta4.value =
                                            "Alertas4: " + (ordenarYConvertir(result.data.Volumen)
                                                ?: result.data.Volumen)
                                    }

                                    "66169bab-d567-4388-b634-357ff0dac5f1" -> {
                                        _bateria.value = "Comando: " + result.data.Volumen
                                    }

                                    else -> Unit

                                }
                                val formato =
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                val fechaActual = ZonedDateTime.now()
                                val fecha = fechaActual.format(formato)

                                if (result.data.Volumen.isNotEmpty()) {
//                                    val defaultSensorResponse = SensorResponse(
//                                        fld_fechaHoraEnvio = fecha,
//                                        fld_latitud = latitude.toString(),
//                                        fld_longitud = longitude.toString(),
//                                        fld_valor = result.data.Volumen,
//                                        id_vehiculo = 1,
//                                        fld_uuid = result.data.SensorId,
//                                        id_usuario = 1
//                                    )

//                                    onTaskCreated(defaultSensorResponse)
                                }
                            }
                            result.data.connectionState
                        }

                        is Resource.Loading -> {
                            initializingMessage = result.message
                            ConnectionState.CurrentlyInitializing
                        }

                        is Resource.Error -> {
                            errorMessage = result.errorMessage
                            ConnectionState.Uninitialized
                        }
                    }
                }
            }
        }
    }


    fun onwrite(defaultSensorResponse: String) {
        viewModelScope.launch {
            writeInitialValuese(defaultSensorResponse, 1)
        }
    }

    fun onwriteEemo(defaultSensorResponse: String) {
        viewModelScope.launch {

            writeInitialValuese(defaultSensorResponse, 32)
        }
    }

    fun onwriteElmo(defaultSensorResponse: String) {
        viewModelScope.launch {

            writeInitialValuese(defaultSensorResponse, 33)
        }
    }

    fun onwritesace(defaultSensorResponse: String) {
        viewModelScope.launch {

            writeInitialValuese(defaultSensorResponse, 34)
        }
    }

    fun onwriteEinc(defaultSensorResponse: String) {
        viewModelScope.launch {

            writeInitialValuese(defaultSensorResponse, 6)
        }
    }

    fun onwriteRTC(defaultSensorResponse: String) {
        viewModelScope.launch {

            writeInitialValuese(defaultSensorResponse, 7)
        }
    }

    private suspend fun dismissProgressDialog() {
        withContext(Dispatchers.Main) {
            alertDialog?.dismiss()
        }
    }

    fun ordenarYConvertir(volumen: String): Float? {

        try {
            val volumenOrdenado = kotlin.text.StringBuilder()

            for (i in volumen.indices step 2) {
                volumenOrdenado.insert(0, volumen.substring(i, i + 2))
            }

            val intBits = Integer.parseInt(volumenOrdenado.toString(), 16)
            return java.lang.Float.intBitsToFloat(intBits)
        } catch (e: NumberFormatException) {

            println(" hexadecimal : $volumen")
            return null
        } catch (e: Exception) {

            println("error d: ${e.message}")
            return null
        }
    }

    private suspend fun showProgressDialog(context: Context) {
        withContext(Dispatchers.Main) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("Enviando información, no cierre la aplicación...")
                .setCancelable(false)
            alertDialog = builder.create()
            alertDialog?.show()
        }
    }

    fun disconnect() {
        _connectionState.update { ConnectionState.Uninitialized }
        sensorReceiveManager.disconnect()
        _Message.value = ""
    }

    fun reconnect() {
        _shouldReconnect.value = true
        sensorReceiveManager.reconnect()
    }


    private val _isHotspotConfigured = MutableStateFlow(false)
    val isHotspotConfigured: StateFlow<Boolean> = _isHotspotConfigured.asStateFlow()

    private val _isConnectedToHotspot = MutableStateFlow(false)
    val isConnectedToHotspot: StateFlow<Boolean> = _isConnectedToHotspot.asStateFlow()


    fun updateHotspotConfigurationStatus(isConfigured: Boolean) {
        _isHotspotConfigured.value = isConfigured
    }

    fun updateHotspotConnectionStatus(isConnected: Boolean) {
        _isConnectedToHotspot.value = isConnected
    }


    private val _isHotspotAvailable = mutableStateOf(false)
    val isHotspotAvailable: State<Boolean> = _isHotspotAvailable

    private val _hotspotConfigurationMessage = mutableStateOf("")
    val hotspotConfigurationMessage: State<String> = _hotspotConfigurationMessage

    suspend fun configureHotspot(ssid: String, password: String, isEnabled: Boolean) {
        try {

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Espere un momento, hasta que se le indique que deba conectar a la red ",
                    Toast.LENGTH_LONG
                ).show()
            }
            _hotspotConfigurationMessage.value = "Configurando red..."

            writesDataHostPost(ssid, password, isEnabled)

            delay(20000)

            onwrite("1")

            delay(1000)

            _isHotspotConfigured.value = true
            _hotspotConfigurationMessage.value = "Hotspot configurado correctamente"

        } catch (e: Exception) {
            _hotspotConfigurationMessage.value = "Error al configurar red: ${e.message}"
            _isHotspotConfigured.value = false
        }
    }


    @SuppressLint("MissingPermission", "ServiceCast")
    fun connectToHotspot(
        ssid: String,
        password: String,
        context: Context,
        onResult: (Boolean) -> Unit
    ) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                            super.onAvailable(network)
                            connectivityManager.bindProcessToNetwork(network)
                            Log.d("Hotspot", "Conectado a $ssid")
                            networkAvailable.complete(true)
                        }

                        override fun onUnavailable() {
                            super.onUnavailable()
                            if (!networkAvailable.isCompleted) {
                                networkAvailable.complete(false)
                            }
                        }
                    }

                    connectivityManager.requestNetwork(request, callback)


                    val result = withTimeoutOrNull(15000L) {
                        networkAvailable.await()
                    } ?: false

                    onResult(result)
                } else {
                    val wifiManager =
                        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

                    val config = WifiConfiguration().apply {
                        SSID = "\"$ssid\""
                        preSharedKey = "\"$password\""
                        allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    }

                    val netId = wifiManager.addNetwork(config)
                    if (netId != -1) {
                        wifiManager.disconnect()
                        wifiManager.enableNetwork(netId, true)
                        wifiManager.reconnect()

                        delay(3000)
                        Log.d("Hotspot", "Conectado a $ssid")
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }


    fun writeInitialValuese(valor: String, opcion: Int) {
        sensorReceiveManager.writeInitialValuese(valor, opcion)
    }


    fun writesDataHostPost(ssid: String, password: String, isWifiEnabled: Boolean) {

        sensorReceiveManager.writesDataHostPost(ssid, password, isWifiEnabled)

    }

    fun clearvalores() {
        errorMessage = null
        _Message.value = ""
        _volumen1.value = ""
        _volumen2.value = ""
        _volumen3.value = ""
        _volumen4.value = ""
        _temperatura1.value = ""
        _temperatura2.value = ""
        _temperatura3.value = ""
        _temperatura4.value = ""
        _temperatura1.value = ""
        _constante1.value = ""
        _constante2.value = ""
        _constante3.value = ""
        _constante4.value = ""
        _fecha1.value = ""
        _fecha2.value = ""
        _bateria.value = ""
        _Message.value = ""
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initializeConnection() {
        val sharedPreferences =
            context.getSharedPreferences("misPreferenciasFSC", Context.MODE_PRIVATE)
        val savedMacAddress = sharedPreferences.getString("mac", "")

        if (!savedMacAddress.isNullOrEmpty()) {
            NetworkConfig.nombreconfiguracion = savedMacAddress
            NetworkConfig.configuracion = "mac"
        }

        errorMessage = null
        _Message.value = ""
        _volumen1.value = ""
        _volumen2.value = ""
        _volumen3.value = ""
        _volumen4.value = ""
        _temperatura1.value = ""
        _temperatura2.value = ""
        _temperatura3.value = ""
        _temperatura4.value = ""
        _temperatura1.value = ""
        _constante1.value = ""
        _constante2.value = ""
        _constante3.value = ""
        _constante4.value = ""
        _fecha1.value = ""
        _fecha2.value = ""
        _bateria.value = ""
        subscribeToChanges()
        sensorReceiveManager.startReceiving()
    }

    override fun onCleared() {
        super.onCleared()
        sensorReceiveManager.closeConnection()
        _Message.value = ""
    }
}