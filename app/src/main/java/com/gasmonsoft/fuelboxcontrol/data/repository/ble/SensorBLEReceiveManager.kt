package com.gasmonsoft.fuelboxcontrol.data.repository.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.gasmonsoft.fuelboxcontrol.data.model.ble.AccelerometerData
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CCCD_UUID
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CHAR_UUID_ACELEROMETRO
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CHAR_UUID_ALERTAS_GLOBALES
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CHAR_UUID_SENSOR_1
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CHAR_UUID_SENSOR_2
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CHAR_UUID_SENSOR_3
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CHAR_UUID_SENSOR_4
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CONTROL_FIRMWARE_UUID
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorData
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorResult
import com.gasmonsoft.fuelboxcontrol.data.model.ble.SensorState
import com.gasmonsoft.fuelboxcontrol.data.model.ble.safeUuidOrNull
import com.gasmonsoft.fuelboxcontrol.data.service.ble.BleConnectionManager
import com.gasmonsoft.fuelboxcontrol.data.service.ble.GattOpQueue
import com.gasmonsoft.fuelboxcontrol.data.service.firmware.CtrlMsg
import com.gasmonsoft.fuelboxcontrol.data.service.firmware.ProFileSender
import com.gasmonsoft.fuelboxcontrol.data.service.firmware.UpgradeFileType
import com.gasmonsoft.fuelboxcontrol.domain.sensor.SensorDataType
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig.configuracion
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig.nombreconfiguracion
import com.gasmonsoft.fuelboxcontrol.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Float.parseFloat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class SensorEvent(
    val type: SensorDataType? = null,
    val value: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorEvent

        if (type != other.type) return false
        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + value.contentHashCode()
        return result
    }
}

@SuppressLint("MissingPermission")
class SensorBLEReceiveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context,
    private val bleConnectionManager: BleConnectionManager,
    private val gattQueue: GattOpQueue,
    private val proFileSender: ProFileSender,
) : SensorReceiveManager {

    private val sharedPrefs = context.getSharedPreferences("ble_prefs", Context.MODE_PRIVATE)
    private val KEY_LAST_MAC = "last_mac"

    private var scanAttempts = 0
    private val MAX_SCAN_ATTEMPTS = 30

    private var RSSI = 0
    private var currentMtu = 23

    private val _sensorEvents = MutableSharedFlow<SensorEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val sensorEvents = _sensorEvents.asSharedFlow()
    private val MAXIMUM_CONNECTION_ATTEMPTS = 6
    private var text = ""
    private var textw = ""

    private var currentCharacteristicIndex = 0
    override val data: MutableSharedFlow<Resource<SensorResult>> = MutableSharedFlow()
    override val discoveredDevices: MutableStateFlow<Set<Device>> =
        MutableStateFlow(emptySet())

    override val connectionState: MutableSharedFlow<ConnectionState> = MutableSharedFlow()
    private var subscriptionIndex = 0
    private var batteryLevel = 0
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    val characteristicsToWrite = mutableListOf<BluetoothGattCharacteristic>()

    private var _sensorState = MutableStateFlow(SensorState())
    override val sensorData = _sensorState.asStateFlow()

    private var gatt: BluetoothGatt? = null
    private var isScanning = false
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val characteristicsWithNotifications = mutableSetOf<BluetoothGattCharacteristic>()
    private val writeableCharacteristics = mutableSetOf<BluetoothGattCharacteristic>()
    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (result.device.name != "DIESEL") return

            val device = result.device

            discoveredDevices.update { devices ->
                devices + Device(
                    mac = device.address,
                    name = device.name ?: "Dispositivo desconocido",
                    rssi = result.rssi
                )
            }

            val shouldConnect = when (configuracion) {
                "mac" -> device.address == nombreconfiguracion.trim()
                else -> false
            }

            if (shouldConnect && isScanning) {
                discoveredDevices.update { emptySet() }
                bleScanner.stopScan(this)
                isScanning = false

                coroutineScope.launch {
                    connectionState.emit(ConnectionState.CurrentlyInitializing)
                    data.emit(Resource.Loading(message = "Conectándose a dispositivo ${result.device.name ?: "desconocido"}"))
                }

                val newGatt = result.device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )
                this@SensorBLEReceiveManager.gatt = newGatt
            } else if (isScanning) {
                scanAttempts++
                if (scanAttempts >= MAX_SCAN_ATTEMPTS) {
                    coroutineScope.launch {
                        connectionState.emit(ConnectionState.Disconnected)
                        data.emit(Resource.Error(errorMessage = "No se encontró el dispositivo $nombreconfiguracion después de $MAX_SCAN_ATTEMPTS intentos"))
                    }
                    isScanning = false
                    scanAttempts = 0
                    bleScanner.stopScan(this)
                }
            }
        }
    }

    private var currentConnectionAttempt = 1

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            this@SensorBLEReceiveManager.gatt = gatt

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    sharedPrefs.edit().putString(KEY_LAST_MAC, gatt.device.address).apply()

                    subscriptionIndex = 0
                    characteristicsWithNotifications.clear()
                    writeableCharacteristics.clear()
                    characteristicsToWrite.clear()
                    currentCharacteristicIndex = 0

                    gatt.requestMtu(517)

                    coroutineScope.launch {
                        _sensorState.value = SensorState()
                        connectionState.emit(ConnectionState.Connected)
                        data.emit(Resource.Loading(message = "Conectado, negociando MTU..."))
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gattQueue.onDisconnected()
                    coroutineScope.launch {
                        connectionState.emit(ConnectionState.Disconnected)
                        data.emit(
                            Resource.Success(
                                data = SensorResult(
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    ConnectionState.Disconnected
                                )
                            )
                        )
                    }
                    bleConnectionManager.setStatus(BluetoothProfile.STATE_DISCONNECTED)
                    bleConnectionManager.clearGatt()
                    reconnect()
                }
            } else {
                Log.i("SensorBLEReceiverManager.kt", "Gatt status error: $status")
                gatt.close()
                currentConnectionAttempt++
                if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                    coroutineScope.launch {
                        connectionState.emit(ConnectionState.CurrentlyInitializing)
                        data.emit(Resource.Loading(message = "Intentando conectarse $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"))
                    }
                    bleConnectionManager.setStatus(BluetoothProfile.STATE_DISCONNECTED)
                    startReceiving()
                } else {
                    gattQueue.onDisconnected()
                    currentConnectionAttempt = 0
                    coroutineScope.launch {
                        connectionState.emit(ConnectionState.Disconnected)
                        data.emit(Resource.Error(errorMessage = "No se pudo conectar al dispositivo BLE"))
                    }
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status == BluetoothGatt.GATT_SUCCESS) RSSI = rssi
            gattQueue.onReadRemoteRssi(rssi, status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val services = gatt.services
                if (services != null) {
                    val characteristics = mutableListOf<BluetoothGattCharacteristic>()
                    services.forEach { service ->
                        val serviceUuid = service.uuid.toString()
                        if (isSensorServiceUUID(serviceUuid)) {
                            characteristics.addAll(service.characteristics)
                        }
                    }
                    coroutineScope.launch {
                        proFileSender.setMtuAndGatt(
                            mtu = currentMtu
                        )
                        subscribeToDiscoveredCharacteristics(gatt, characteristics)
                    }
                }
            } else {
                val errorMessage = "Error al descubrir los servicios: $status"
                coroutineScope.launch {
                    data.emit(Resource.Error(errorMessage = errorMessage))
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
                with(characteristic) {
                    handleCharacteristic(UUID.fromString(uuid.toString()), value, gatt)
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (gatt == null) return
                currentMtu = mtu
                proFileSender.updateMtu(mtu)
                coroutineScope.launch {
                    bleConnectionManager.setStatus(BluetoothProfile.STATE_CONNECTED)
                    data.emit(Resource.Loading(message = "MTU Negociado: $mtu. Descubriendo servicios..."))
                }
                val mac = sharedPrefs.getString(KEY_LAST_MAC, "")
                if (mac.isNullOrBlank()) return
                bleConnectionManager.setGatt(gatt, mac)
                gatt.discoverServices()

                coroutineScope.launch {
                    gattQueue.readRemoteRssiAwait(gatt)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                with(characteristic) {
                    handleCharacteristic(UUID.fromString(uuid.toString()), value, gatt)
                }
            }
        }

        private fun handleCharacteristic(uuid: UUID, value: ByteArray, gatt: BluetoothGatt) {
            when (uuid) {
                CHAR_UUID_SENSOR_1 -> {
                    val sensorData = value.toString(Charsets.UTF_8).trim().split(" ")
                    if (sensorData.size == 5) {
                        _sensorState.update { current ->
                            current.copy(
                                sensor1 = getSensorData(sensorData)
                            )
                        }
                    } else {
                        _sensorState.update {
                            it.copy(
                                sensor1 = setErrorSensor(value.toString(Charsets.UTF_8))
                            )
                        }
                    }
                    _sensorEvents.tryEmit(SensorEvent(SensorDataType.FIRST, value))
                }

                CHAR_UUID_SENSOR_2 -> {
                    val sensorData = value.toString(Charsets.UTF_8).trim().split(" ")
                    if (sensorData.size == 5) {
                        _sensorState.update { current ->
                            current.copy(
                                sensor2 = getSensorData(sensorData)
                            )
                        }
                    } else {
                        _sensorState.update { current ->
                            current.copy(
                                sensor2 = setErrorSensor(value.toString(Charsets.UTF_8))
                            )
                        }
                    }
                    _sensorEvents.tryEmit(SensorEvent(SensorDataType.SECOND, value))

                }

                CHAR_UUID_SENSOR_3 -> {
                    val sensorData = value.toString(Charsets.UTF_8).trim().split(" ")
                    if (sensorData.size == 5) {
                        _sensorState.update { current ->
                            current.copy(
                                sensor3 = getSensorData(sensorData)
                            )
                        }
                    } else {
                        _sensorState.update {
                            it.copy(
                                sensor3 = setErrorSensor(value.toString(Charsets.UTF_8))
                            )
                        }
                    }
                    _sensorEvents.tryEmit(SensorEvent(SensorDataType.THIRD, value))
                }

                CHAR_UUID_SENSOR_4 -> {
                    val sensorData = value.toString(Charsets.UTF_8).trim().split(" ")
                    if (sensorData.size == 5) {
                        _sensorState.update { current ->
                            current.copy(
                                sensor4 = getSensorData(sensorData)
                            )
                        }
                    } else {
                        _sensorState.update {
                            it.copy(
                                sensor4 = setErrorSensor(value.toString(Charsets.UTF_8))
                            )
                        }
                    }
                    _sensorEvents.tryEmit(SensorEvent(SensorDataType.FOURTH, value))
                }

                CHAR_UUID_ACELEROMETRO -> {
                    val sensorData = value.toString(Charsets.UTF_8).split(" ")
                    if (sensorData.size == 3) {
                        _sensorState.update { current ->
                            current.copy(
                                acelerometro = getAcelerometroData(sensorData)
                            )
                        }
                    } else {
                        _sensorState.update { current ->
                            current.copy(
                                acelerometro = setErrorAccelerometer(value.toString(Charsets.UTF_8))
                            )
                        }
                    }
                    _sensorEvents.tryEmit(SensorEvent(SensorDataType.ACCELEROMETER, value))
                }

                CHAR_UUID_ALERTAS_GLOBALES -> {
                    val sensorData = value.toString(Charsets.UTF_8)
                    _sensorState.update { current ->
                        current.copy(
                            alertas = sensorData
                        )
                    }
                }

                CONTROL_FIRMWARE_UUID -> {
                    proFileSender.onControlNotify(value)
                }
            }

            val hexValue = value.joinToString("") { byte -> "%02x".format(byte) }
            val resultado = hexValue
            val sensorResult = when (uuid.toString()) {
                NetworkConfig.Volumen1_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                "0fe0e4d2-724e-4e1a-bebe-79e29f621b15",
                "80c4c443-2128-4570-b0da-6b3dbced01a6",
                NetworkConfig.Temperatura1_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Constante1_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Fecha1_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Alertas1_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Volumen2_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Temperatura2_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Constante2_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Fecha2_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Alertas2_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Volumen3_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Temperatura3_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Constante3_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Fecha3_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Alertas3_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Volumen4_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Temperatura4_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Constante4_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Fecha4_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                NetworkConfig.Alertas4_CHARACTERISTICS_UUID.takeIf { it.isNotEmpty() },
                    -> {
                    SensorResult(
                        uuid.toString(),
                        resultado,
                        resultado,
                        resultado,
                        resultado,
                        resultado,
                        ConnectionState.Connected
                    )
                }

                else -> return
            }

            coroutineScope.launch {
                data.emit(Resource.Success(data = sensorResult))
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            gattQueue.onCharacteristicWrite(characteristic, status)
        }

        private fun getAcelerometroData(sensorData: List<String>): AccelerometerData {
            val date = "${sensorData[0]} ${sensorData[1]}"
            return AccelerometerData(
                date = date,
                value = sensorData.drop(2).last()
            )
        }

        private fun setErrorAccelerometer(rawData: String): AccelerometerData {
            return AccelerometerData(
                rawData = rawData,
                error = true
            )
        }

        private fun getSensorData(sensorData: List<String>): SensorData {
            val date = "${sensorData[0]} ${sensorData[1]}"
            val data = sensorData.drop(2)
            val isError = data.any {
                data.size >= 3 && data[2] == "-555" && data[1] == "-555" && data[0] == "-555"
            }
            return SensorData(
                date = date,
                temperatura = if (data.size >= 3) data[2] else "",
                volumen = if (data.isNotEmpty()) data[0] else "",
                calidad = if (data.size >= 2) data[1] else "",
                error = isError,
                rawData = data.joinToString(",")
            )
        }

        private fun setErrorSensor(rawData: String): SensorData {
            return SensorData(
                rawData = rawData,
                error = rawData.isNotEmpty()
            )
        }

        private fun subscribeToDiscoveredCharacteristics(
            gatt: BluetoothGatt,
            characteristics: List<BluetoothGattCharacteristic>
        ) {
            characteristics.forEach { characteristic ->
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    characteristicsWithNotifications.add(characteristic)
                }
                if (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                    writeableCharacteristics.add(characteristic)
                }
            }
            subscribeToCharacteristics(gatt, characteristicsWithNotifications)
        }

        private fun subscribeToCharacteristics(
            gatt: BluetoothGatt,
            characteristics: Set<BluetoothGattCharacteristic>,
        ) {
            val descriptorUuid =
                safeUuidOrNull(NetworkConfig.Notificacion1_DESCRIPTOR_UUID) ?: CCCD_UUID

            val list = characteristics
                .filter { ch ->
                    val props = ch.properties
                    (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
                            (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
                }
                .toList()

            while (subscriptionIndex < list.size) {
                val ch = list[subscriptionIndex]
                val descriptor =
                    ch.getDescriptor(descriptorUuid)
                        ?: ch.getDescriptor(CCCD_UUID)

                if (descriptor == null) {
                    subscriptionIndex++
                    continue
                }

                val notificationSet = gatt.setCharacteristicNotification(ch, true)
                if (!notificationSet) {
                    subscriptionIndex++
                    continue
                }

                val enableValue =
                    if (ch.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    else
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                val writeStarted = if (Build.VERSION.SDK_INT >= 33) {
                    gatt.writeDescriptor(
                        descriptor,
                        enableValue
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    descriptor.value = enableValue
                    gatt.writeDescriptor(descriptor)
                }

                if (writeStarted) {
                    return
                } else {
                    subscriptionIndex++
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            gattQueue.onDescriptorWrite(descriptor, status)
        }
    }

    override fun startReceiving() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        discoveredDevices.update { emptySet() }
        scanAttempts = 0

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)

        if (nombreconfiguracion.isBlank()) {
            val lastMac = sharedPrefs.getString(KEY_LAST_MAC, "")
            if (!lastMac.isNullOrBlank()) {
                nombreconfiguracion = lastMac
                configuracion = "mac"
            }
        }

        connectedDevices.forEach { device ->
            if (device.name == "DIESEL") {
                discoveredDevices.update { devices ->
                    devices + Device(
                        mac = device.address,
                        name = device.name ?: "Dispositivo conectado",
                        rssi = -50
                    )
                }
            }
        }

        val phantomDevice = connectedDevices.find { it.address == nombreconfiguracion.trim() }
        if (phantomDevice != null && configuracion == "mac") {
            Log.i(
                "SensorBLEReceiveManager",
                "Dispositivo fantasma detectado: ${phantomDevice.address}. Reconectando..."
            )
            coroutineScope.launch {
                connectionState.emit(ConnectionState.CurrentlyInitializing)
                data.emit(Resource.Loading(message = "Reconectando a dispositivo conocido..."))
            }
            gatt = phantomDevice.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )
            return
        }

        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Escaneando dispositivo BLE..."))
        }
        isScanning = true
        if (bluetoothAdapter.isEnabled) {
            bleScanner.startScan(null, scanSettings, scanCallback)
        }
    }

    override fun reconnect() {
        gattQueue.onDisconnected()

        gatt?.disconnect()
        gatt?.close()
        gatt = null

        bleConnectionManager.clearGatt()

        startReceiving()
    }

    override suspend fun sendConfFile(
        data: ByteArray,
        name: String,
        sensorId: String,
        upgradeType: UpgradeFileType
    ): Pair<CtrlMsg?, String> {

        return try {
            val sender = proFileSender
            val result = withContext(Dispatchers.IO) {
                sender.sendFilePro(
                    fileName = name,
                    fileBytes = data,
                    upgradeType = upgradeType,
                    sensorId = if (sensorId == "0") "" else sensorId
                )
            }

            Log.d("SensorBLEReceiver", "Transferencia OK ✅")
            Pair(result, "")

        } catch (e: Exception) {

            Log.d("SensorBLEReceiver", "Transferencia falló: ${e.message}")
            Pair(null, "Transferencia falló: ${e.message}")
        }
    }

    override fun disconnect() {
        nombreconfiguracion = ""
        configuracion = ""
        sharedPrefs.edit().remove(KEY_LAST_MAC).apply()
        gattQueue.onDisconnected()

        gatt?.disconnect()
        gatt?.close()
        gatt = null

        bleConnectionManager.clearGatt()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun writeInitialValuese(valor: String, opcion: Int) {
        coroutineScope.launch {
            val characteristic = writeableCharacteristics.find {
                it.uuid == UUID.fromString("66169bab-d567-4388-b634-357ff0dac5f1")
            } ?: return@launch

            val payload = when (opcion) {
                1 -> "[COMM] ${valor.trim()}"
                34 -> "[SACE] ${valor.trim()}"
                32 -> "[EEMO] ${valor.trim()}"
                33 -> "[ELMO] ${parseFloat(valor.trim())}"
                6 -> "[EINC] ${valor.trim()}"
                2 -> {
                    val currentGatt = gatt ?: return@launch
                    val rssi = gattQueue.readRemoteRssiAwait(currentGatt) ?: RSSI
                    batteryLevel = getBatteryLevel(context)
                    "[INFO] $rssi , $batteryLevel"
                }

                3 -> {
                    val fechaHoraActual = LocalDateTime.now()
                    val formato = DateTimeFormatter.ofPattern("ss,mm,HH,dd,MM,yyyy,")
                    val fechaHoraFormateada = fechaHoraActual.format(formato)
                    "[NTP]$fechaHoraFormateada"
                }

                7 -> {
                    val fechaHoraActual = LocalDateTime.now()
                    val formato = when (valor) {
                        "0" -> "yyyy,MM,dd"
                        "1" -> "HH,mm,ss"
                        else -> ""
                    }
                    val fechaHoraFormateada = if (formato.isNotEmpty()) {
                        fechaHoraActual.format(DateTimeFormatter.ofPattern(formato))
                    } else ""
                    "[RTC] $valor,$fechaHoraFormateada"
                }

                else -> null
            }

            if (payload != null) {
                val currentGatt = gatt ?: return@launch
                val ok = gattQueue.writeCharacteristicAwait(
                    currentGatt,
                    characteristic,
                    payload.toByteArray(Charsets.UTF_8)
                )

                if (ok) {
                    data.emit(
                        Resource.Success(
                            data = SensorResult(
                                characteristic.uuid.toString(),
                                "Enviado: $payload",
                                "", "", "", "",
                                ConnectionState.Connected
                            )
                        )
                    )
                }
            }
        }
    }

    override fun writesDataHostPost(ssid: String, password: String, isWifiEnabled: Boolean) {
        coroutineScope.launch {
            val g = this@SensorBLEReceiveManager.gatt
            if (g == null) {
                data.emit(Resource.Error(errorMessage = "Gatt es null, no se puede escribir. ¿Ya conectaste?"))
                return@launch
            }
            resetWriteState()
            val ssidUuid = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb")
            val passwordUuid = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
            val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

            writeableCharacteristics.forEach { ch ->
                when (ch.uuid) {
                    ssidUuid -> {
                        ch.value = ssid.trim().toByteArray()
                        characteristicsToWrite.add(ch)
                    }

                    passwordUuid -> {
                        ch.value = password.trim().toByteArray()
                        characteristicsToWrite.add(ch)
                    }

                    wifiEnabledUuid -> {
                        ch.value = (if (isWifiEnabled) "1" else "0").toByteArray()
                        characteristicsToWrite.add(ch)
                    }
                }
            }
            if (characteristicsToWrite.isNotEmpty()) {
                writeNextCharacteristic(g, characteristicsToWrite, 0)
            }
        }
    }

    private fun writeNextCharacteristic(
        gatt: BluetoothGatt,
        characteristicsToWrite: List<BluetoothGattCharacteristic>,
        index: Int
    ) {
        if (index < characteristicsToWrite.size) {
            val ch = characteristicsToWrite[index]
            val value = ch.value ?: return

            coroutineScope.launch {
                val ok = gattQueue.writeCharacteristicAwait(
                    gatt = gatt,
                    ch = ch,
                    value = value,
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )

                if (ok) {
                    data.emit(
                        Resource.Success(
                            data = SensorResult(
                                ch.uuid.toString(),
                                "Escribiendo ${ch.uuid}",
                                "", "", "", "",
                                ConnectionState.Connected
                            )
                        )
                    )
                }
            }
        } else {
            coroutineScope.launch {
                data.emit(
                    Resource.Success(
                        data = SensorResult(
                            "",
                            "Todos los datos enviados correctamente.",
                            "", "", "", "",
                            ConnectionState.Connected
                        )
                    )
                )
            }
            resetWriteState()
        }
    }

    private fun resetWriteState() {
        currentCharacteristicIndex = 0
        characteristicsToWrite.clear()
    }

    override fun closeConnection() {
        bleScanner.stopScan(scanCallback)
        gatt?.close()
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isSensorServiceUUID(uuid: String): Boolean {
        return uuid == "bd508a75-88ce-4ea5-8d15-0644e8fd237a" ||
                uuid == "b944612f-d98d-4baa-a81d-20473cfcf383" ||
                uuid == "487a5fee-d25e-49fd-aa7a-92c292dee4b6" ||
                uuid == "69ad5dc1-e2b5-43cb-9c3c-494c1876d86d" ||
                uuid == "00001800-0000-1000-8000-00805f9b34fb" ||
                uuid == "cae8124d-a939-422a-a0af-a74313013811"
    }
}
