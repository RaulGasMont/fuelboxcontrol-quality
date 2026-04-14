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
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CCCD_UUID
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CHAR_UUID_SENSOR_1
import com.gasmonsoft.fuelboxcontrol.data.model.ble.CONTROL_FIRMWARE_UUID
import com.gasmonsoft.fuelboxcontrol.data.model.ble.ConnectionState
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
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
    private val MAXIMUM_CONNECTION_ATTEMPTS = 6
    private val INITIALIZATION_TIMEOUT_MS = 15000L

    private var currentConnectionAttempt = 1
    private var currentMtu = 23
    private var rssiValue = 0
    private var batteryLevel = 0
    private var isScanning = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var initializationTimeoutJob: Job? = null

    private val bleScanner by lazy { bluetoothAdapter.bluetoothLeScanner }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    private val characteristicsWithNotifications = mutableSetOf<BluetoothGattCharacteristic>()
    private val writeableCharacteristics = mutableSetOf<BluetoothGattCharacteristic>()

    private val _sensorEvents = MutableSharedFlow<SensorEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val sensorEvents = _sensorEvents.asSharedFlow()

    override val data: MutableSharedFlow<Resource<SensorResult>> =
        MutableSharedFlow(replay = 1, extraBufferCapacity = 8)

    override val discoveredDevices: MutableStateFlow<Set<Device>> =
        MutableStateFlow(emptySet())

    override val connectionState: MutableSharedFlow<ConnectionState> =
        MutableSharedFlow(replay = 1, extraBufferCapacity = 1)

    private val _sensorState = MutableStateFlow(SensorState())
    override val sensorData = _sensorState.asStateFlow()

    private fun startInitializationTimeout() {
        initializationTimeoutJob?.cancel()
        initializationTimeoutJob = scope.launch {
            delay(INITIALIZATION_TIMEOUT_MS)
            if (connectionState.replayCache.lastOrNull() == ConnectionState.CurrentlyInitializing) {
                Log.w("SensorBLEReceiver", "Initialization timeout reached. Resetting connection.")
                teardownGatt(clearMac = false)
                connectionState.emit(ConnectionState.Disconnected)
                data.emit(Resource.Error(errorMessage = "Tiempo de espera de conexión agotado. Intente nuevamente."))
            }
        }
    }

    private fun cancelInitializationTimeout() {
        initializationTimeoutJob?.cancel()
        initializationTimeoutJob = null
    }

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
                discoveredDevices.value = emptySet()
                bleScanner.stopScan(this)
                isScanning = false

                scope.launch {
                    connectionState.emit(ConnectionState.CurrentlyInitializing)
                    data.emit(
                        Resource.Loading(
                            message = "Conectándose a dispositivo ${device.name ?: "desconocido"}"
                        )
                    )
                }

                val newGatt = device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )

                bleConnectionManager.attachConnectingGatt(device.address, newGatt)
                return
            }

            if (isScanning) {
                scanAttempts++
                if (scanAttempts >= MAX_SCAN_ATTEMPTS) {
                    isScanning = false
                    scanAttempts = 0
                    bleScanner.stopScan(this)

                    scope.launch {
                        cancelInitializationTimeout()
                        connectionState.emit(ConnectionState.Disconnected)
                        data.emit(
                            Resource.Error(
                                errorMessage = "No se encontró el dispositivo $nombreconfiguracion después de $MAX_SCAN_ATTEMPTS intentos"
                            )
                        )
                    }
                }
            } else {
                scanAttempts = 0
                scope.launch {
                    connectionState.emit(ConnectionState.Disconnected)
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (!isCurrentGatt(gatt) && newState != BluetoothProfile.STATE_CONNECTED) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                runCatching { gatt.close() }
                currentConnectionAttempt++

                if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                    bleConnectionManager.markError()
                    scope.launch {
                        connectionState.emit(ConnectionState.CurrentlyInitializing)
                        data.emit(
                            Resource.Loading(
                                message = "Intentando conectarse $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"
                            )
                        )
                    }
                    startReceiving()
                } else {
                    cancelInitializationTimeout()
                    gattQueue.onDisconnected()
                    currentConnectionAttempt = 1
                    bleConnectionManager.markError()
                    bleConnectionManager.clearConnection(clearMac = false)

                    scope.launch {
                        connectionState.emit(ConnectionState.Disconnected)
                        data.emit(Resource.Error(errorMessage = "No se pudo conectar al dispositivo BLE"))
                    }
                }
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    sharedPrefs.edit().putString(KEY_LAST_MAC, gatt.device.address).apply()

                    currentConnectionAttempt = 1
                    currentMtu = 23
                    resetDiscoveredState()
                    _sensorState.value = SensorState()

                    bleConnectionManager.attachConnectingGatt(gatt.device.address, gatt)
                    bleConnectionManager.markNegotiatingMtu()

                    scope.launch {
                        connectionState.emit(ConnectionState.CurrentlyInitializing)
                        data.emit(Resource.Loading(message = "Conectado, negociando MTU..."))
                    }

                    gatt.requestMtu(517)
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    val shouldReconnect = bleConnectionManager.shouldReconnectAfterDisconnect()

                    teardownGatt(
                        gattToClose = gatt,
                        clearMac = !shouldReconnect
                    )

                    scope.launch {
                        cancelInitializationTimeout()
                        connectionState.emit(ConnectionState.Disconnected)
                        data.emit(
                            Resource.Success(
                                data = SensorResult(
                                    "", "", "", "", "", "",
                                    ConnectionState.Disconnected
                                )
                            )
                        )
                    }

                    if (shouldReconnect) {
                        startReceiving()
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (!isCurrentGatt(gatt)) return
            super.onMtuChanged(gatt, mtu, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                scope.launch {
                    data.emit(Resource.Error(errorMessage = "No se pudo negociar MTU"))
                }
                return
            }

            currentMtu = mtu
            proFileSender.updateMtu(mtu)
            bleConnectionManager.markDiscoveringServices(mtu)

            scope.launch {
                data.emit(Resource.Loading(message = "MTU negociado: $mtu. Descubriendo servicios..."))
            }

            gatt.discoverServices()

            scope.launch {
                gattQueue.readRemoteRssiAwait(gatt)?.let { rssiValue = it }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            if (status == BluetoothGatt.GATT_SUCCESS) rssiValue = rssi
            gattQueue.onReadRemoteRssi(rssi, status)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (!isCurrentGatt(gatt)) return

            if (status != BluetoothGatt.GATT_SUCCESS) {
                scope.launch {
                    data.emit(Resource.Error(errorMessage = "Error al descubrir servicios: $status"))
                }
                return
            }

            val characteristics = buildList {
                gatt.services.orEmpty().forEach { service ->
                    if (isSensorServiceUUID(service.uuid.toString())) {
                        addAll(service.characteristics)
                    }
                }
            }

            bleConnectionManager.markSubscribing()

            scope.launch {
                proFileSender.setMtuAndGatt(mtu = currentMtu)

                val subscribed = subscribeToDiscoveredCharacteristics(
                    gatt = gatt,
                    characteristics = characteristics
                )

                if (!subscribed) {
                    data.emit(Resource.Error(errorMessage = "No se pudieron habilitar notificaciones"))
                    return@launch
                }

                bleConnectionManager.markReady()
                cancelInitializationTimeout()

                connectionState.emit(ConnectionState.Connected)
                data.emit(Resource.Loading(message = "Dispositivo listo para operar"))
                data.emit(
                    Resource.Success(
                        data = SensorResult(
                            "",
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

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            if (!isCurrentGatt(gatt)) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                handleCharacteristic(
                    uuid = UUID.fromString(characteristic.uuid.toString()),
                    value = value,
                    gatt = gatt
                )
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            gattQueue.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            gattQueue.onDescriptorWrite(gatt, descriptor, status)
        }
    }

    override fun startReceiving() {
        teardownGatt(clearMac = false)

        discoveredDevices.value = emptySet()
        scanAttempts = 0

        val targetMac = resolveTargetMac()
        if (!targetMac.isNullOrBlank()) {
            sharedPrefs.edit().putString(KEY_LAST_MAC, targetMac).apply()
        }
        bleConnectionManager.startNewScan(targetMac)

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        
        val hasConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val connectedDevices = if (hasConnectPermission) {
            bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        } else {
            emptyList()
        }

        val phantomDevice = connectedDevices.find { it.address == targetMac }

        if (phantomDevice != null) {
            scope.launch {
                connectionState.emit(ConnectionState.CurrentlyInitializing)
                data.emit(Resource.Loading(message = "Reconectando a dispositivo conocido..."))
                startInitializationTimeout()
            }

            val newGatt = phantomDevice.connectGatt(
                context,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
            )

            bleConnectionManager.attachConnectingGatt(phantomDevice.address, newGatt)
            return
        }

        scope.launch {
            connectionState.emit(ConnectionState.CurrentlyInitializing)
            data.emit(Resource.Loading(message = "Escaneando dispositivo BLE..."))
            startInitializationTimeout()
        }

        isScanning = true
        if (bluetoothAdapter.isEnabled) {
            val hasScanPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (hasScanPermission) {
                bleScanner.startScan(null, scanSettings, scanCallback)
            } else {
                Log.e("SensorBLEReceiver", "No se puede iniciar el escaneo: Falta permiso BLUETOOTH_SCAN")
                isScanning = false
            }
        }
    }

    override fun reconnect() {
        teardownGatt(clearMac = false)
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
        scope.launch {
            cancelInitializationTimeout()
            connectionState.emit(ConnectionState.Disconnected)
        }
        bleConnectionManager.markDisconnectingByUser()
        teardownGatt(clearMac = true)
    }

    override fun writeInitialValuese(valor: String, opcion: Int) {
        scope.launch {
            if (!isReadyForGattOps()) {
                data.emit(Resource.Error(errorMessage = "BLE no está listo para escribir"))
                return@launch
            }

            val gatt = currentGatt() ?: return@launch

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
                    val rssi = gattQueue.readRemoteRssiAwait(gatt) ?: rssiValue
                    batteryLevel = getBatteryLevel(context)
                    "[INFO] $rssi , $batteryLevel"
                }

                3 -> {
                    val fechaHoraActual = LocalDateTime.now()
                    val formato = DateTimeFormatter.ofPattern("ss,mm,HH,dd,MM,yyyy,")
                    "[NTP]${fechaHoraActual.format(formato)}"
                }

                7 -> {
                    val fechaHoraActual = LocalDateTime.now()
                    val formato = when (valor) {
                        "0" -> "yyyy,MM,dd"
                        "1" -> "HH,mm,ss"
                        else -> ""
                    }
                    val fechaHoraFormateada =
                        if (formato.isNotEmpty()) fechaHoraActual.format(
                            DateTimeFormatter.ofPattern(
                                formato
                            )
                        )
                        else ""
                    "[RTC] $valor,$fechaHoraFormateada"
                }

                else -> null
            }

            if (payload == null) return@launch

            val ok = gattQueue.writeCharacteristicAwait(
                gatt = gatt,
                ch = characteristic,
                value = payload.toByteArray(Charsets.UTF_8)
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
            } else {
                data.emit(Resource.Error(errorMessage = "No se pudo enviar el payload"))
            }
        }
    }

    override fun writesDataHostPost(ssid: String, password: String, isWifiEnabled: Boolean) {
        scope.launch {
            if (!isReadyForGattOps()) {
                data.emit(Resource.Error(errorMessage = "BLE no está listo para escribir"))
                return@launch
            }

            val gatt = currentGatt() ?: run {
                data.emit(Resource.Error(errorMessage = "Gatt es null"))
                return@launch
            }

            val ssidUuid = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb")
            val passwordUuid = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
            val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

            val payloads = listOf(
                ssidUuid to ssid.trim().toByteArray(),
                passwordUuid to password.trim().toByteArray(),
                wifiEnabledUuid to (if (isWifiEnabled) "1" else "0").toByteArray()
            )

            val writableByUuid = writeableCharacteristics.associateBy { it.uuid }

            for ((uuid, value) in payloads) {
                val characteristic = writableByUuid[uuid] ?: continue

                val ok = gattQueue.writeCharacteristicAwait(
                    gatt = gatt,
                    ch = characteristic,
                    value = value,
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )

                if (!ok) {
                    data.emit(Resource.Error(errorMessage = "Falló la escritura de $uuid"))
                    return@launch
                }

                data.emit(
                    Resource.Success(
                        data = SensorResult(
                            characteristic.uuid.toString(),
                            "Escribiendo ${characteristic.uuid}",
                            "", "", "", "",
                            ConnectionState.Connected
                        )
                    )
                )
            }

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
    }

    override fun closeConnection() {
        teardownGatt(clearMac = false)
    }

    private suspend fun subscribeToDiscoveredCharacteristics(
        gatt: BluetoothGatt,
        characteristics: List<BluetoothGattCharacteristic>
    ): Boolean {
        characteristicsWithNotifications.clear()
        writeableCharacteristics.clear()

        characteristics.forEach { characteristic ->
            val props = characteristic.properties

            if ((props and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 ||
                (props and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
            ) {
                characteristicsWithNotifications.add(characteristic)
            }

            if ((props and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                (props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
            ) {
                writeableCharacteristics.add(characteristic)
            }
        }

        val descriptorUuid =
            safeUuidOrNull(NetworkConfig.Notificacion1_DESCRIPTOR_UUID) ?: CCCD_UUID

        val notifiableChars = characteristicsWithNotifications.toList()

        for (ch in notifiableChars) {
            val descriptor = ch.getDescriptor(descriptorUuid)
                ?: ch.getDescriptor(CCCD_UUID)
                ?: continue

            val notificationEnabled = gatt.setCharacteristicNotification(ch, true)
            if (!notificationEnabled) return false

            val enableValue =
                if ((ch.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) {
                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                } else {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                }

            val ok = gattQueue.writeDescriptorAwait(
                gatt = gatt,
                desc = descriptor,
                value = enableValue
            )

            if (!ok) return false
        }

        return true
    }

    private fun getBatteryLevel(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun currentGatt(): BluetoothGatt? = bleConnectionManager.currentGatt()

    private fun isCurrentGatt(gatt: BluetoothGatt): Boolean {
        return bleConnectionManager.currentGatt() === gatt
    }

    private fun resetDiscoveredState() {
        characteristicsWithNotifications.clear()
        writeableCharacteristics.clear()
    }

    private fun resolveTargetMac(): String? {
        if (nombreconfiguracion.isBlank()) {
            val lastMac = sharedPrefs.getString(KEY_LAST_MAC, "")
            if (!lastMac.isNullOrBlank()) {
                nombreconfiguracion = lastMac
                configuracion = "mac"
            }
        }

        return if (configuracion == "mac" && nombreconfiguracion.isNotBlank()) {
            nombreconfiguracion.trim()
        } else {
            null
        }
    }

    private fun teardownGatt(
        gattToClose: BluetoothGatt? = bleConnectionManager.currentGatt(),
        clearMac: Boolean
    ) {
        cancelInitializationTimeout()
        gattQueue.onDisconnected()

        resetDiscoveredState()
        discoveredDevices.value = emptySet()

        runCatching {
            if (isScanning) {
                bleScanner.stopScan(scanCallback)
                isScanning = false
            }
        }

        runCatching { gattToClose?.disconnect() }
        runCatching { gattToClose?.close() }

        bleConnectionManager.clearConnection(clearMac = clearMac)
    }

    private fun isReadyForGattOps(): Boolean {
        return bleConnectionManager.isReady() && currentGatt() != null
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

        scope.launch {
            data.emit(Resource.Success(data = sensorResult))
        }
    }

    private fun isSensorServiceUUID(uuid: String): Boolean {
        return uuid == "bd508a75-88ce-4ea5-8d15-0644e8fd237a" ||
                uuid == "b944612f-d98d-4baa-a81d-20473cfcf383" ||
                uuid == "487a5fee-d25e-49fd-aa7a-92c292dee4b6" ||
                uuid == "69ad5dc1-e2b5-43cb-9c3c-494c1876d86d" ||
                uuid == "00001800-0000-1000-8000-00805f9b34fb" ||
                uuid == "cae8124d-a939-422a-a0af-a74313013811" ||
                uuid == "958ae39e-2fb0-4389-9e63-0c68cb134426"
    }
}