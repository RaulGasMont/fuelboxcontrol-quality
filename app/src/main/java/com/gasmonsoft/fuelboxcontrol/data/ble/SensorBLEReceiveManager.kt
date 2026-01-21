package com.gasmonsoft.fuelboxcontrol.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig.configuracion
import com.gasmonsoft.fuelboxcontrol.utils.NetworkConfig.nombreconfiguracion
import com.gasmonsoft.fuelboxcontrol.utils.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.lang.Float.parseFloat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject


@SuppressLint("MissingPermission")

class SensorBLEReceiveManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context,

    ) : SensorReceiveManager {
    private var scanAttempts = 0
    private val MAX_SCAN_ATTEMPTS = 30

    private var RSSI = 0


    private val MAXIMUM_CONNECTION_ATTEMPTS = 6
    private var text = ""
    private var textw = ""

    private var currentCharacteristicIndex = 0
    override val data: MutableSharedFlow<Resource<SensorResult>> = MutableSharedFlow()
    private var subscriptionIndex = 0
    private var batteryLevel = 0
    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }
    val characteristicsToWrite = mutableListOf<BluetoothGattCharacteristic>()

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
            val device = result.device

            // Escanea los dispositivos en busca de la MAC correspondiente a la caja
            val shouldConnect = when (configuracion) {
                "mac" -> device.address == nombreconfiguracion.trim()
                else -> false
            }

            if (shouldConnect && isScanning) {
                bleScanner.stopScan(this)

                isScanning = false

                // Envia al resto de la App el estado de conexión
                coroutineScope.launch {
                    data.emit(Resource.Loading(message = "Conectándose a dispositivo ${result.device.name}"))
                }

                // Establece la conexión Bluetooth con la MAC especificada
                result.device.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
                )

            } else if (isScanning) {
                scanAttempts++

                if (scanAttempts >= MAX_SCAN_ATTEMPTS) {
                    coroutineScope.launch {
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
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
                    this@SensorBLEReceiveManager.gatt = gatt
                    gatt.readRemoteRssi()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    coroutineScope.launch {
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

                    // Metodo encargado de la reconexión
                    reconnect()
                }
            } else {
                Log.i("SensorBLEReceiverManager.kt", "Estado de BluetoothGatt No Conectado.")
                gatt.close()
                currentConnectionAttempt++
                if (currentConnectionAttempt <= MAXIMUM_CONNECTION_ATTEMPTS) {
                    coroutineScope.launch {
                        data.emit(Resource.Loading(message = "Intentando conectarse $currentConnectionAttempt/$MAXIMUM_CONNECTION_ATTEMPTS"))
                    }
                    startReceiving()
                } else {
                    currentConnectionAttempt = 0
                    coroutineScope.launch {
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
            val hexValue = value.joinToString("") { byte -> "%02x".format(byte) }
            val resultado = hexValue
            val sensorResult = when (uuid) {
                UUID.fromString(NetworkConfig.Volumen1_CHARACTERISTICS_UUID),
                UUID.fromString("0fe0e4d2-724e-4e1a-bebe-79e29f621b15"),
                UUID.fromString("80c4c443-2128-4570-b0da-6b3dbced01a6"),
                UUID.fromString(NetworkConfig.Temperatura1_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Constante1_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Fecha1_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Alertas1_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Volumen2_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Temperatura2_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Constante2_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Fecha2_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Alertas2_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Volumen3_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Temperatura3_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Constante3_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Fecha3_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Alertas3_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Volumen4_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Temperatura4_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Constante4_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Fecha4_CHARACTERISTICS_UUID),
                UUID.fromString(NetworkConfig.Alertas4_CHARACTERISTICS_UUID),
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
            var valor = ""

            Log.d("SensorBLEReceiveManager", "${characteristic.uuid}")

            // Estatus 133 representa que no hubo tiempo de concretar la operacion
            if (status == 0 || status == 133) {
                valor = "Se realizo con éxito$text"
                if (characteristic.uuid == UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb") || characteristic.uuid == UUID.fromString(
                        "00002a25-0000-1000-8000-00805f9b34fb"
                    ) || characteristic.uuid == UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
                ) {
                    valor = "Se realizo con éxito"
                    currentCharacteristicIndex++
                    coroutineScope.launch {
                        Log.d("SensorBLEReceiveManager", "$characteristicsToWrite")
                        writeNextCharacteristic(
                            gatt!!,
                            characteristicsToWrite,
                            currentCharacteristicIndex
                        )
                    }
                }
            } else if (status == 1) {
                valor = "No se realizó$text"

                if (characteristic.uuid == UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb") || characteristic.uuid == UUID.fromString(
                        "00002a25-0000-1000-8000-00805f9b34fb"
                    ) || characteristic.uuid == UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
                ) {

                    resetWriteState()
                }
            }


            val sensorResult = when (characteristic.uuid) {
                UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb"),

                UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb"),

                UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb"),

                UUID.fromString("95b3b0f2-d277-4690-8e2a-691e1ebd927d"),

                UUID.fromString("463290d6-431e-416a-b303-6564bec8800f"),

                UUID.fromString("66169bab-d567-4388-b634-357ff0dac5f1"),
                    -> {

                    SensorResult(
                        characteristic.uuid.toString(),
                        valor,
                        valor,
                        valor,
                        valor,
                        valor,
                        ConnectionState.Connected
                    )
                }

                else -> return
            }

            coroutineScope.launch {
                data.emit(Resource.Success(data = sensorResult))


            }
            if (characteristic.uuid == UUID.fromString("463290d6-431e-416a-b303-6564bec8800f")) {

            }
            if (characteristic.uuid == UUID.fromString("95b3b0f2-d277-4690-8e2a-691e1ebd927d")) {

            }

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
            characteristics: MutableSet<BluetoothGattCharacteristic>,
        ) {
            val filteredCharacteristics = characteristics.toList()

            while (subscriptionIndex < filteredCharacteristics.size) {
                val characteristic = filteredCharacteristics[subscriptionIndex]
                val descriptor =
                    characteristic.getDescriptor(UUID.fromString(NetworkConfig.Notificacion1_DESCRIPTOR_UUID))

                if (descriptor != null) {
                    gatt.setCharacteristicNotification(characteristic, true)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                    gatt.writeDescriptor(descriptor)

                    break
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
            val ssidUuid = UUID.fromString("00002a23-0000-1000-8000-00805f9b34fb")
            val passwordUuid = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
            val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")



            writeableCharacteristics.forEach { characteristic ->
                when (characteristic.uuid) {
                    ssidUuid -> {
                        val value = ssid.trim()
                        characteristic.setValue(value.toByteArray())
                        textw = value
                        characteristicsToWrite.add(characteristic)

                    }

                    passwordUuid -> {
                        val value = password.trim()
                        textw = value
                        characteristic.setValue(value.toByteArray())
                        characteristicsToWrite.add(characteristic)

                    }

                    wifiEnabledUuid -> {
                        val value = (if (isWifiEnabled) '1' else '0').toString()

                        characteristic.setValue(value.toString().toByteArray())
                        characteristicsToWrite.add(characteristic)

                    }
                }
            }

            if (characteristicsToWrite.isNotEmpty()) {
                writeNextCharacteristic(gatt, characteristicsToWrite, currentCharacteristicIndex)
            } else {
                coroutineScope.launch {
                    data.emit(
                        Resource.Success(
                            data = SensorResult(
                                ssidUuid.toString(),
                                "No se encontraron características válidas para escribir.",
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

    private suspend fun writeNextCharacteristic(
        gatt: BluetoothGatt?,
        characteristicsToWrite: List<BluetoothGattCharacteristic>,
        currentCharacteristicIndex: Int
    ) {

        val wifiEnabledUuid = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

        if (currentCharacteristicIndex < characteristicsToWrite.size) {
            val characteristic = characteristicsToWrite[currentCharacteristicIndex]
            val success = gatt?.writeCharacteristic(characteristic)

            if (success == true) {
                coroutineScope.launch {
                    data.emit(
                        Resource.Success(
                            data = SensorResult(
                                characteristic.uuid.toString(),
                                "Escribiendo ${characteristic.uuid}",
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
                                characteristic.uuid.toString(),
                                "Error al escribir ${characteristic.uuid}",
                                "",
                                "",
                                "",
                                "",
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
                            "",
                            "",
                            "",
                            "",
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

    fun getBluetoothSignalStrength(callback: RssiCallback) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        val deviceAddress = gatt?.device?.address

        if (deviceAddress != null) {
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
            val bluetoothGatt =
                bluetoothDevice.connectGatt(context, false, object : BluetoothGattCallback() {
                    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                        super.onReadRemoteRssi(gatt, rssi, status)
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            callback.onRssiReceived(rssi)
                        } else {
                            Log.e("Bluetooth", "Error al obtener RSSI")
                            callback.onRssiReceived(0)
                        }
                    }
                })

            bluetoothGatt.readRemoteRssi()
        }
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

