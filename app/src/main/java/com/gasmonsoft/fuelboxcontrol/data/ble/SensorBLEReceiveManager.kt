package com.gasmonsoft.fuelboxcontrol.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
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
import com.gasmonsoft.fuelboxcontrol.domain.SensorDataType
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

    ) : SensorReceiveManager {
    private var scanAttempts = 0
    private val MAX_SCAN_ATTEMPTS = 30

    private var RSSI = 0

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

            // Escanea los dispositivos en busca de la MAC correspondiente a la caja
            val shouldConnect = when (configuracion) {
                "mac" -> device.address == nombreconfiguracion.trim()
                else -> false
            }

            if (shouldConnect && isScanning) {
                discoveredDevices.update { emptySet() }

                bleScanner.stopScan(this)

                isScanning = false

                // Envia al resto de la App el estado de conexión
                coroutineScope.launch {
                    connectionState.emit(ConnectionState.CurrentlyInitializing)
                    data.emit(Resource.Loading(message = "Conectándose a dispositivo ${result.device.name ?: "desconocido"}"))
                }

                // Establece la conexión Bluetooth con la MAC especificada
//                val bluetoothManager =
//                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//                val bluetoothAdapter = bluetoothManager.adapter
//                bluetoothAdapter.getRemoteDevice(mac)
//                    .connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
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
            // Guarda SIEMPRE el gatt que te da Android
            this@SensorBLEReceiveManager.gatt = gatt

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    // Limpia estado de descubrimiento por si vienes de reconexión
                    subscriptionIndex = 0
                    characteristicsWithNotifications.clear()
                    writeableCharacteristics.clear()
                    characteristicsToWrite.clear()
                    currentCharacteristicIndex = 0

                    gatt.requestMtu(517)

                    coroutineScope.launch {
                        _sensorState.value = SensorState()
                        connectionState.emit(ConnectionState.Connected)
                        data.emit(Resource.Loading(message = "Conectado, descubriendo servicios..."))
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
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
                    startReceiving()
                } else {
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
                        subscribeToDiscoveredCharacteristics(gatt, characteristics)
                    }
                } else {

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

                Log.i(
                    "SensorBLEReceiverManager.kt",
                    "Estado del Perfil Bluetooth como Conectado."
                )

                // Confirma la conexión con el dispositivo, alertando sobre la busqueda de sus servicios
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Conectado, descubriendo servicios..."))
                }

                // Realiza la busqueda de los servicios

                gatt.discoverServices()
                gatt.readRemoteRssi()
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

            // Guarda gatt por si acaso
            this@SensorBLEReceiveManager.gatt = gatt

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // OK -> avanza cadena si aplica
                if (
                    characteristic.uuid == UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb") ||
                    characteristic.uuid == UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb") ||
                    characteristic.uuid == UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
                ) {
                    currentCharacteristicIndex++
                    coroutineScope.launch {
                        writeNextCharacteristic(
                            gatt,
                            characteristicsToWrite,
                            currentCharacteristicIndex
                        )
                    }
                }
            } else {
                // Error real -> resetea o reintenta
                Log.e(
                    "SensorBLEReceiveManager",
                    "Write FAIL uuid=${characteristic.uuid} status=$status"
                )
                resetWriteState()
            }
        }

        private fun getAcelerometroData(sensorData: List<String>): AccelerometerData {
            val date = "${sensorData[0]} ${sensorData[1]}"
            return AccelerometerData(
                date = date,
                value = sensorData.drop(2).last()
            )
        }

        private fun getSensorData(sensorData: List<String>): SensorData {
            val date = "${sensorData[0]} ${sensorData[1]}"
            val data = sensorData.drop(2)
            return SensorData(
                date = date,
                temperatura = data[2],
                volumen = data[0],
                calidad = data[1]
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

        private fun writeInitialValues(
            gatt: BluetoothGatt,
            toList: List<BluetoothGattCharacteristic>
        ) {
            writeableCharacteristics.forEach { characteristic ->

                when (characteristic.uuid) {
                    UUID.fromString("463290d6-431e-416a-b303-6564bec8800f") -> {
                        characteristic.value = RSSI.toString().toByteArray(Charsets.UTF_8)
                        coroutineScope.launch {
                            data.emit(
                                Resource.Success(
                                    data = SensorResult(
                                        "463290d6-431e-416a-b303-6564bec8800f",
                                        "Se envia senial $RSSI",
                                        "",
                                        "",
                                        "",
                                        "",
                                        ConnectionState.Connected
                                    )
                                )
                            )
                        }

                        val success = gatt.writeCharacteristic(characteristic)

                        if (success) {


                        } else {
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "463290d6-431e-416a-b303-6564bec8800f",
                                            "Success: $success" +
                                                    "",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                        }
                    }

                    UUID.fromString("95b3b0f2-d277-4690-8e2a-691e1ebd927d") -> {
                        batteryLevel = getBatteryLevel(context)

                        characteristic.value = batteryLevel.toString().toByteArray(Charsets.UTF_8)
                        val success = gatt.writeCharacteristic(characteristic)

                        if (success) {
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "95b3b0f2-d277-4690-8e2a-691e1ebd927d",
                                            "bateria $batteryLevel  Success:$success",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                        } else {
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "95b3b0f2-d277-4690-8e2a-691e1ebd927d",
                                            "Se envia bateria $batteryLevel  Success:$success",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }


        private fun subscribeToCharacteristics(
            gatt: BluetoothGatt,
            characteristics: Set<BluetoothGattCharacteristic>,
        ) {
            // 1) Usa el UUID configurado si es válido, si no usa CCCD (0x2902)
            val descriptorUuid =
                safeUuidOrNull(NetworkConfig.Notificacion1_DESCRIPTOR_UUID) ?: CCCD_UUID

            // 2) Filtra solo características que soporten NOTIFY o INDICATE
            val list = characteristics
                .filter { ch ->
                    val props = ch.properties
                    (props and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
                            (props and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
                }
                .toList()

            while (subscriptionIndex < list.size) {
                val ch = list[subscriptionIndex]

                // 3) Obtén el descriptor (preferido: configurado; fallback: CCCD)
                val descriptor =
                    ch.getDescriptor(descriptorUuid)
                        ?: ch.getDescriptor(CCCD_UUID)

                if (descriptor == null) {
                    subscriptionIndex++
                    continue
                }

                // 4) Activa notificación a nivel GATT
                val notificationSet = gatt.setCharacteristicNotification(ch, true)
                if (!notificationSet) {
                    subscriptionIndex++
                    continue
                }

                // 5) El valor depende si es NOTIFY o INDICATE
                val enableValue =
                    if (ch.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
                        BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    else
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                // 6) Escribe descriptor (Android 13+ vs legacy)
                val writeStarted = if (Build.VERSION.SDK_INT >= 33) {
                    // writeDescriptor(descriptor, value) -> int
                    gatt.writeDescriptor(descriptor, enableValue) == BluetoothStatusCodes.SUCCESS
                } else {
                    descriptor.value = enableValue
                    gatt.writeDescriptor(descriptor)
                }

                if (writeStarted) {
                    // IMPORTANTE: no incrementes aquí.
                    // Incrementa en onDescriptorWrite cuando sea SUCCESS y luego llama de nuevo a subscribeToCharacteristics().
                    return
                } else {
                    // Si no pudo iniciar, intenta la siguiente para evitar quedarte atorado.
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                subscriptionIndex++
                subscribeToCharacteristics(
                    gatt,
                    characteristicsWithNotifications
                )
            }
        }
    }

    override fun startReceiving() {
        coroutineScope.launch {
            data.emit(Resource.Loading(message = "Escaneando dispositivo BLE..."))
        }
        isScanning = true
        if (bluetoothAdapter.isEnabled) {
            bleScanner.startScan(null, scanSettings, scanCallback)
        }
    }

    override fun reconnect() {
        gatt?.connect()
    }

    override fun disconnect() {
        nombreconfiguracion = ""
        configuracion = ""
        gatt?.disconnect()
        gatt?.close()
        gatt = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun writeInitialValuese(valor: String, opcion: Int) {
        coroutineScope.launch {

            writeableCharacteristics.forEach { characteristic ->
                when (characteristic.uuid) {
                    UUID.fromString("66169bab-d567-4388-b634-357ff0dac5f1") -> {

                        if (opcion == 1) {
                            var x = "[COMM] ${valor.trim()}";
                            text = x;
                            characteristic.value =
                                x.toByteArray(Charsets.UTF_8)
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "66169bab-d567-4388-b634-357ff0dac5f1",
                                            "Se envia COMM $text",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                            val success = gatt?.writeCharacteristic(characteristic)

                            if (success == true) {


                            } else {
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "66169bab-d567-4388-b634-357ff0dac5f1",
                                                "Success: $text$success" +
                                                        "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                            }
                        }
                        if (opcion == 34) {
                            var x = "[SACE] ${valor.trim()}";
                            text = x;
                            characteristic.value =
                                x.toByteArray(Charsets.UTF_8)
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "66169bab-d567-4388-b634-357ff0dac5f1",
                                            "Se envia SACE $text",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                            val success = gatt?.writeCharacteristic(characteristic)

                            if (success == true) {

                            } else {
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "66169bab-d567-4388-b634-357ff0dac5f1",
                                                "Success: $text$success" +
                                                        "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                            }
                        }
                        if (opcion == 32) {
                            var x = "[EEMO] ${valor.trim()}";
                            text = x;
                            characteristic.value =
                                x.toByteArray(Charsets.UTF_8)
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "66169bab-d567-4388-b634-357ff0dac5f1",
                                            "Se envia EEMO $text",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                            val success = gatt?.writeCharacteristic(characteristic)

                            if (success == true) {


                            } else {
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "66169bab-d567-4388-b634-357ff0dac5f1",
                                                "Success: $text$success" +
                                                        "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                            }
                        }
                        if (opcion == 33) {
                            var valorFlotante =
                                parseFloat(valor.trim());  // Convertimos a flotante y eliminamos espacios
                            var x = "[ELMO] ${valorFlotante}";
                            text = x;
                            characteristic.value =
                                x.toByteArray(Charsets.UTF_8)
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "66169bab-d567-4388-b634-357ff0dac5f1",
                                            "Se envia ELMO $text",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                            val success = gatt?.writeCharacteristic(characteristic)

                            if (success == true) {


                            } else {
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "66169bab-d567-4388-b634-357ff0dac5f1",
                                                "Success: $text$success" +
                                                        "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                            }
                        }
                        //einc

                        if (opcion == 6) {
                            var x = "[EINC] ${valor.trim()}";
                            text = x;

                            characteristic.value =
                                x.toByteArray(Charsets.UTF_8)
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "66169bab-d567-4388-b634-357ff0dac5f1",
                                            "Se envia EINC $text",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                            val success = gatt?.writeCharacteristic(characteristic)

                            if (success == true) {


                            } else {
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "66169bab-d567-4388-b634-357ff0dac5f1",
                                                "Success: EINC$success" +
                                                        "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                            }
                        }

                        //BATERIA
                        if (opcion == 2) {
                            if (gatt != null) {
                                gatt?.readRemoteRssi()
                                batteryLevel = getBatteryLevel(context)


                                var x = "[INFO] $RSSI , $batteryLevel";
                                text = x;
                                characteristic.value =
                                    x.toByteArray(Charsets.UTF_8)
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "66169bab-d567-4388-b634-357ff0dac5f1",
                                                "Se envia bat senial $RSSI,$batteryLevel",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                                val success = gatt?.writeCharacteristic(characteristic)

                                if (success == true) {


                                } else {
                                    coroutineScope.launch {
                                        data.emit(
                                            Resource.Success(
                                                data = SensorResult(
                                                    "66169bab-d567-4388-b634-357ff0dac5f1",
                                                    "Success: $text$success" +
                                                            "",
                                                    "",
                                                    "",
                                                    "",
                                                    "",
                                                    ConnectionState.Connected
                                                )
                                            )
                                        )
                                    }

                                }
                            }
                        }

//HORA
                        if (opcion == 3) {
                            val fechaHoraActual = LocalDateTime.now()

                            val formato = DateTimeFormatter.ofPattern("ss,mm,HH,dd,MM,yyyy,")

                            val fechaHoraFormateada = fechaHoraActual.format(formato)
                            var x = "[NTP]$fechaHoraFormateada";
                            text = x;
                            characteristic.value =
                                x.toByteArray(Charsets.UTF_8)
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "463290d6-431e-416a-b303-6564bec8800f",
                                            "Se envia fecha$x",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                            val success = gatt?.writeCharacteristic(characteristic)

                            if (success == true) {


                            } else {
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "463290d6-431e-416a-b303-6564bec8800f",
                                                "Success: $success" +
                                                        "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                            }
                        }


                        //rtc

                        if (opcion == 7) {
                            val fechaHoraActual = LocalDateTime.now()
                            var fechaHoraFormateada = ""


                            val formato = when (valor) {
                                "0" -> "yyyy,MM,dd"
                                "1" -> "HH,mm,ss"
                                else -> ""
                            }


                            if (formato.isNotEmpty()) {
                                val dateTimeFormatter = DateTimeFormatter.ofPattern(formato)



                                fechaHoraFormateada = fechaHoraActual.format(dateTimeFormatter)


                            } else {

                            }


                            var x = "[RTC] $valor,$fechaHoraFormateada";
                            text = x;
                            characteristic.value =
                                x.toByteArray(Charsets.UTF_8)
                            coroutineScope.launch {
                                data.emit(
                                    Resource.Success(
                                        data = SensorResult(
                                            "66169bab-d567-4388-b634-357ff0dac5f1",
                                            "Se envia fecha$x",
                                            "",
                                            "",
                                            "",
                                            "",
                                            ConnectionState.Connected
                                        )
                                    )
                                )
                            }

                            val success = gatt?.writeCharacteristic(characteristic)

                            if (success == true) {


                            } else {
                                coroutineScope.launch {
                                    data.emit(
                                        Resource.Success(
                                            data = SensorResult(
                                                "66169bab-d567-4388-b634-357ff0dac5f1",
                                                "Success: $success" +
                                                        "",
                                                "",
                                                "",
                                                "",
                                                "",
                                                ConnectionState.Connected
                                            )
                                        )
                                    )
                                }

                            }
                        }
                    }


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

            // Reset correcto antes de armar la lista
            resetWriteState()

            val ssidUuid = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb")
            val passwordUuid = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
            val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

            writeableCharacteristics.forEach { ch ->
                when (ch.uuid) {
                    ssidUuid -> {
                        val value = ssid.trim().toByteArray()
                        ch.value = value
                        characteristicsToWrite.add(ch)
                    }

                    passwordUuid -> {
                        val value = password.trim().toByteArray()
                        ch.value = value
                        characteristicsToWrite.add(ch)
                    }

                    wifiEnabledUuid -> {
                        val value = (if (isWifiEnabled) "1" else "0").toByteArray()
                        ch.value = value
                        characteristicsToWrite.add(ch)
                    }
                }
            }

            if (characteristicsToWrite.isNotEmpty()) {
                writeNextCharacteristic(g, characteristicsToWrite, currentCharacteristicIndex)
            } else {
                data.emit(
                    Resource.Success(
                        data = SensorResult(
                            ssidUuid.toString(),
                            "No se encontraron características válidas para escribir.",
                            "", "", "", "",
                            ConnectionState.Connected
                        )
                    )
                )
            }
        }
    }

    private fun writeCharacteristicCompat(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray
    ): Boolean {
        val supportsNoResp =
            (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0

        val writeType =
            if (supportsNoResp) BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

        return if (Build.VERSION.SDK_INT >= 33) {
            gatt.writeCharacteristic(
                characteristic,
                payload,
                writeType
            ) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = writeType
            characteristic.value = payload
            gatt.writeCharacteristic(characteristic)
        }
    }

    private fun writeNextCharacteristic(
        gatt: BluetoothGatt,
        characteristicsToWrite: List<BluetoothGattCharacteristic>,
        currentCharacteristicIndex: Int
    ) {
        val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

        if (currentCharacteristicIndex < characteristicsToWrite.size) {
            val ch = characteristicsToWrite[currentCharacteristicIndex]

            val payload = ch.value ?: byteArrayOf()
            val started = writeCharacteristicCompat(gatt, ch, payload)

            if (started) {
                coroutineScope.launch {
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
            } else {
                coroutineScope.launch {
                    data.emit(
                        Resource.Success(
                            data = SensorResult(
                                ch.uuid.toString(),
                                "No se pudo iniciar write (GATT busy o char inválida) ${ch.uuid}",
                                "", "", "", "",
                                ConnectionState.Disconnected
                            )
                        )
                    )
                }
                resetWriteState()
            }
        } else {
            coroutineScope.launch {
                data.emit(
                    Resource.Success(
                        data = SensorResult(
                            wifiEnabledUuid.toString(),
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

    interface RssiCallback {
        fun onRssiReceived(rssi: Int)
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
