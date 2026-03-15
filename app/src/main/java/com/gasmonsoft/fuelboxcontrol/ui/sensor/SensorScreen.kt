package com.gasmonsoft.fuelboxcontrol.ui.sensor

import android.Manifest
import android.app.DatePickerDialog
import android.bluetooth.BluetoothAdapter
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.data.ble.AccelerometerData
import com.gasmonsoft.fuelboxcontrol.data.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorData
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorState
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.dataprocessing.EncriptarBin
import com.gasmonsoft.fuelboxcontrol.utils.dataprocessing.ReducirDatosSensor
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorRoute(
    onBack: () -> Unit,
    viewModelSensor: SensorViewModel = hiltViewModel()
) {
    val bleConnectionState = viewModelSensor.connectionState.collectAsState().value
    val sensorInfoState by viewModelSensor.sensorInfoState.collectAsState()

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val sensorMessage: String by viewModelSensor.Message.observeAsState(initial = "")
    val constante1: String by viewModelSensor.constante1.observeAsState("")
    val fecha1: String by viewModelSensor.fecha1.observeAsState("")
    val alerta1: String by viewModelSensor.alerta1.observeAsState("")

    val constante2: String by viewModelSensor.constante2.observeAsState("")
    val fecha2: String by viewModelSensor.fecha2.observeAsState("")
    val alerta2: String by viewModelSensor.alerta2.observeAsState("")

    val constante3: String by viewModelSensor.constante3.observeAsState("")
    val alerta3: String by viewModelSensor.alerta3.observeAsState("")

    val constante4: String by viewModelSensor.constante4.observeAsState("")
    val fecha4: String by viewModelSensor.fecha4.observeAsState("")
    val alerta4: String by viewModelSensor.alerta4.observeAsState("")
    val bateria: String by viewModelSensor.bateria.observeAsState("")

    val sendingState by viewModelSensor.sendingState.collectAsState()

    SensorScreenContent(
        onBack = onBack,
        bleConnectionState = bleConnectionState,
        sensorInfoState = sensorInfoState,
        permissionState = permissionState,
        sensorMessage = sensorMessage,
        constante1 = constante1,
        fecha1 = fecha1,
        alerta1 = alerta1,
        constante2 = constante2,
        fecha2 = fecha2,
        alerta2 = alerta2,
        constante3 = constante3,
        alerta3 = alerta3,
        constante4 = constante4,
        fecha4 = fecha4,
        alerta4 = alerta4,
        bateria = bateria,
        initializingMessage = viewModelSensor.initializingMessage,
        errorMessage = viewModelSensor.errorMessage,
        sensoruno = viewModelSensor.sensoruno,
        sendingState = sendingState,
        onDisconnect = { viewModelSensor.disconnect() },
        onReconnect = { viewModelSensor.reconnect() },
        onInitializeConnection = { viewModelSensor.initializeConnection() },
        onWritesDataHostPost = { ssid, pass, enabled ->
            viewModelSensor.writesDataHostPost(
                ssid,
                pass,
                enabled
            )
        },
        onWrite = { viewModelSensor.onwrite(it) },
        onWriteEinc = { viewModelSensor.onwriteEinc(it) },
        onWriteRTC = { viewModelSensor.onwriteRTC(it) },
        onWriteEemo = { viewModelSensor.onwriteEemo(it) },
        onWriteElmo = { viewModelSensor.onwriteElmo(it) },
        onWriteSace = { viewModelSensor.onwritesace(it) },
        onClearValores = { viewModelSensor.clearvalores() },
        onSendRead = { context, uri, date, idcaja ->
//            viewModelSensor.onSendRead(
//                context,
//                uri,
//                date,
//                idcaja
//            )
        },
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SensorScreenContent(
    onBack: () -> Unit,
    bleConnectionState: ConnectionState,
    sensorInfoState: SensorState,
    permissionState: MultiplePermissionsState,
    sensorMessage: String,
    constante1: String,
    fecha1: String,
    alerta1: String,
    constante2: String,
    fecha2: String,
    alerta2: String,
    constante3: String,
    alerta3: String,
    constante4: String,
    fecha4: String,
    alerta4: String,
    bateria: String,
    initializingMessage: String?,
    errorMessage: String?,
    sensoruno: String,
    sendingState: String,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onInitializeConnection: () -> Unit,
    onWritesDataHostPost: (String, String, Boolean) -> Unit,
    onWrite: (String) -> Unit,
    onWriteEinc: (String) -> Unit,
    onWriteRTC: (String) -> Unit,
    onWriteEemo: (String) -> Unit,
    onWriteElmo: (String) -> Unit,
    onWriteSace: (String) -> Unit,
    onClearValores: () -> Unit,
    onSendRead: (Context, Uri, String, String) -> Unit
) {
    BackHandler {
        onBack()
        onDisconnect()
    }


    LaunchedEffect(permissionState) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    if (!permissionState.allPermissionsGranted) {
        Text(
            text = "Please grant the required permissions to use Bluetooth features.",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        SystemBroadcastReceiver(systemAction = BluetoothAdapter.ACTION_STATE_CHANGED) { bluetoothState ->
            val action = bluetoothState?.action ?: return@SystemBroadcastReceiver
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                onReconnect()
            }
        }
    }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }


    val lifecycleOwner = LocalLifecycleOwner.current

    var textValue by remember { mutableStateOf("") }
    var textValueEemo by remember { mutableStateOf("") }
    var textValueElmo by remember { mutableStateOf("") }
    var textValueSACE by remember { mutableStateOf("") }
    var textValueRTC by remember { mutableStateOf("") }
    var textValueEINC by remember { mutableStateOf("") }
    //puntowifi
    val isWifiEnabled = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()


    val context = LocalContext.current
    var ip by remember { mutableStateOf("192.168.4.1") }
    var port by remember { mutableStateOf("80") }
    var filePath by remember { mutableStateOf("/sd/muestras/2024/11/11.txt") }
    var filePathDel by remember { mutableStateOf("/sd/muestras/2024/11/11.txt") }
    var message by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var messagestatus by remember { mutableStateOf(" ") }


    var limit by remember { mutableStateOf("1") }
    var jsonConfig by remember { mutableStateOf("") }
    var act by remember { mutableStateOf("") }

    var fileNameWithExtension by remember { mutableStateOf("") }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                val json = inputStream?.bufferedReader().use { it?.readText() } ?: ""
                jsonConfig = json
            }
        }

    val launcheract =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                val contentResolver = context.contentResolver


                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileNameWithExtension =
                                it.getString(nameIndex)
                        }
                    }
                }

                val inputStream = context.contentResolver.openInputStream(it)
                val json = inputStream?.bufferedReader().use { it?.readText() } ?: ""
                act = json
            }
        }

    DisposableEffect(
        key1 = lifecycleOwner,
        effect = {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) {
                    permissionState.launchMultiplePermissionRequest()
                    if (permissionState.allPermissionsGranted && bleConnectionState == ConnectionState.Disconnected) {
                        onReconnect()
                    }
                }
                if (event == Lifecycle.Event.ON_STOP) {
                    if (bleConnectionState == ConnectionState.Connected) {
                        onDisconnect()
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    )

    LaunchedEffect(key1 = permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            if (bleConnectionState == ConnectionState.Uninitialized) {
                onInitializeConnection()
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .wrapContentSize()
            .background(Color.White)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text(
                    text = "DATOS DE CAJA DE COMUNICACIONES",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "------DESCONECTAR BLUETOOTH------",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = TextStyle(fontSize = 16.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            onDisconnect()
                            onBack()
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Text("Desconectar Bluetooth")

                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "------------ENVÍO EINC------------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "El comando EINC calibra el acelerometro con base a la posicion actual del vehiculo.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                )
                Text(
                    text = "Los valores aceptados son: x, y, z",
                    fontStyle = FontStyle.Italic
                )
                TextField(
                    value = textValueEINC,
                    onValueChange = {

                        textValueEINC = it
                    },
                    modifier = Modifier

                        .padding(end = 8.dp),
                    singleLine = true,
                )
                Button(
                    onClick = { onWriteEinc(textValueEINC) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Text("Enviar EINC")
                }
            }

        }
//// RTC

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "------------ENVÍO RTC------------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text(
                    text = "El comando RTC actualiza con la fecha y hora actual la caja de comunicaciones.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    )
                )
                Text(
                    text = "Enviar 0 para calibrar fecha y 1 para calibrar hora.",
                    fontStyle = FontStyle.Italic
                )
            }
            Row(
                modifier = Modifier
                    .padding(2.dp)
                    .wrapContentSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


                TextField(
                    value = textValueRTC,

                    onValueChange = {
                        textValueRTC = it
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                )

                Button(
                    onClick = {
                        val enteredText = textValueRTC
                        onWriteRTC(enteredText)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.purple_500),
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Enviar RTC", style = TextStyle(
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "-----------CARACTERISTÍCAS-----------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                if (bateria.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Text(
                            text = bateria,
                            style = TextStyle(
                                color = colorResource(id = R.color.gray),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                }


                if (constante1.isNotEmpty()) {
                    Text(
                        text = sensorMessage,
                        style = TextStyle(
                            color = colorResource(id = R.color.purple_500),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            SensorDataCard(
                numSensor = "1",
                sensorData = sensorInfoState.sensor1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorDataCard(
                numSensor = "2",
                sensorData = sensorInfoState.sensor2,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorDataCard(
                numSensor = "3",
                sensorData = sensorInfoState.sensor3,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SensorDataCard(
                numSensor = "4",
                sensorData = sensorInfoState.sensor4,
            )
            Spacer(modifier = Modifier.height(8.dp))
            SingleSensorDataCard(
                accelerometerData = sensorInfoState.acelerometro,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

private fun connectToEsp32(
    ip: String,
    port: String,
    message: String,
    context: Context,
    onConnectionResult: (Boolean) -> Unit
) {
    if (ip.isBlank() || port.isBlank() || message.isBlank()) {
        Toast.makeText(context, "Por favor complete todos los campos", Toast.LENGTH_LONG).show()
        return
    }

    val portNumber = port.toIntOrNull()
    if (portNumber == null) {
        Toast.makeText(context, "Número de puerto no válido", Toast.LENGTH_LONG).show()
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = Socket(ip, portNumber)
            socket.soTimeout = 8000

            val out = PrintWriter(socket.getOutputStream(), true)
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))

            out.println("SEND $message")

            val response = input.readLine()




            onConnectionResult(true)
            socket.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Mensaje enviado $response", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            onConnectionResult(false)
        }
    }
}


fun sendValorToESP32(ip: String, port: String, context: Context, valor: String) {


    CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = Socket(ip, port.toInt())
            socket.soTimeout = 19000
            val outputStream = PrintWriter(socket.getOutputStream(), true)
            val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
            outputStream.println("RST $valor")

            val response = inputStream.readLine()
            outputStream.close()
            inputStream.close()
            socket.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Respuesta del ESP32 al enviar RST: $response",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error RST : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}


private fun sendLimits(ip: String, port: String, limitId: String, context: Context) {
    val formattedData = createFormattedData()
    connectAndSendLimite(ip, port, "SEND limite $limitId", context, formattedData)
}

private fun connectAndSendLimite(
    ip: String,
    port: String,
    command: String,
    context: Context,
    data: String
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = Socket(ip, port.toInt())
            socket.soTimeout = 19000

            val out = PrintWriter(socket.getOutputStream(), true)
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Comando
            out.println(command)
            out.println(data)
            out.println(" ok")
            val response = input.readLine()
            out.close()
            input.close()
            socket.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Respuesta del ESP32 LIMITE: $response",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al enviar conf: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(
    label: String,
    onDateSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf("") }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "%04d/%02d/%02d".format(
                    selectedYear,
                    selectedMonth + 1,
                    selectedDay
                )
                selectedDate = formattedDate
                onDateSelected(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    TextField(
        value = selectedDate,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        enabled = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clickable {
                datePickerDialog.show()
            }
    )
}

private fun createFormattedData(): String {
    val nSerie = "FFFFFFFF-1"
    val fecha = "19/09/2024"
    val hora = "01:00 p. m."
    val intervalos = 6
    val limite = "Pendiente"
    val ordenada = "Ordenada"


    val data = listOf(
        listOf(0, 1.302966, -65.62668),
        listOf(51.8342, 1.302966, -65.62668),
        listOf(55.0413, 1.334868, -67.316989),
        listOf(58.1501, 1.331219, -66.994097),
        listOf(61.3308, 1.322596, -66.450228),
        listOf(64.5152, 1.33675, -67.300593)
    )


    val stringBuilder = StringBuilder()


    stringBuilder.append("N.Serie\t$nSerie\n")
    stringBuilder.append("Fecha\t$fecha\n")
    stringBuilder.append("Hora\t$hora\n")
    stringBuilder.append("Intervalos\t$intervalos\n")
    stringBuilder.append("Limite\t$limite\t$ordenada\n")


    for (entry in data) {
        stringBuilder.append("${entry.joinToString("\t")}\n")
    }

    return stringBuilder.toString()
}


fun sendConfigToESP32(ip: String, port: String, context: Context, jsonConfig: String) {


    CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = Socket(ip, port.toInt())
            socket.soTimeout = 19000
            val outputStream = PrintWriter(socket.getOutputStream(), true)
            val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
            outputStream.println("SEND config")

            outputStream.println(jsonConfig)

            outputStream.println(" ok")
            val response = inputStream.readLine()
            outputStream.close()
            inputStream.close()
            socket.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Respuesta del ESP32: $response", Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error conf : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun sendaCTToESP32(
    ip: String,
    port: String,
    context: Context,
    file: String,
    nombrearchivo: String
) {


    CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = Socket(ip, port.toInt())
            socket.soTimeout = 19000
            val outputStream = PrintWriter(socket.getOutputStream(), true)
            val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))

            outputStream.println("SEND actFw $nombrearchivo")

            outputStream.println(file)

            outputStream.println(" ok")
            val response = inputStream.readLine()
            outputStream.close()
            inputStream.close()
            socket.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Respuesta del ESP32 $nombrearchivo: $response",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error conf : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun sendDelToESP32(ip: String, port: String, context: Context, path: String) {


    CoroutineScope(Dispatchers.IO).launch {
        try {
            val socket = Socket(ip, port.toInt())
            socket.soTimeout = 19000
            val outputStream = PrintWriter(socket.getOutputStream(), true)
            val inputStream = BufferedReader(InputStreamReader(socket.getInputStream()))
            outputStream.println("DEL $path")

            val response = inputStream.readLine()
            outputStream.close()
            inputStream.close()
            socket.close()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Respuesta del ESP32 al enviar DEL: $response",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error DEL : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
fun encryptData(reducedData: String): ByteArray {
    return try {
        EncriptarBin().encryptText(reducedData)
    } catch (e: Exception) {
        throw Exception("Error al encriptar los datos: ${e.message}")
    }
}

fun reduceData(decryptedData: ByteArray): String {
    val resultS = StringBuilder()

    try {
        val reader = BufferedReader(decryptedData.inputStream().reader())
        val result = ReducirDatosSensor.reducir2(
            dOperar = 100,
            dDiesmar = 100,
            reader = reader
        )

        val arrays = result.datos.toList()
        val fechas = result.fecha.toList()
        val maxLength = arrays[0].second.size

        for (i in 0 until maxLength) {
            resultS.append(fechas.getOrElse(i) { "" }).append("\t")
            for (array in arrays) {
                resultS.append(array.second.getOrElse(i) { "" }).append("\t")
            }
            resultS.append("\n")
        }
    } catch (e: Exception) {
        throw Exception("Error procesando al procesar los datos: ${e.message}")
    }

    return resultS.toString()
}

fun saveFileToAppWifi(context: Context, fileData: ByteArray, fileName: String): Uri? {
    return try {
        val contentUri = MediaStore.Files.getContentUri("external")
        var fileUri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/AppWifi"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }


            fileUri = context.contentResolver.insert(contentUri, values)


            fileUri?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(fileData)
                }

                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)
            }
        } else {

            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = File(directory, fileName)
            FileOutputStream(file).use { os ->
                os.write(fileData)
            }
            fileUri = Uri.fromFile(file)
        }

        fileUri // Return the URI of the created file
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }
}

fun fetchFileFromServer(
    ip: String,
    port: Int,
    filePath: String,
    fileName: String,
    context: Context
): Uri? {
    return try {
        Socket(ip, port).use { socket ->
            val out = BufferedOutputStream(socket.getOutputStream())
            val input = socket.getInputStream()

            out.write("READ $filePath\n".toByteArray())
            out.flush()

            deleteFileIfExists(context, fileName)

            val filesCollection = MediaStore.Files.getContentUri("external")

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/AppWifi"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(filesCollection, values) ?: run {
                return null
            }

            context.contentResolver.openOutputStream(uri)?.use { os ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    os.write(buffer, 0, bytesRead)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)
            }


            uri
        }
    } catch (e: IOException) {
        null
    } catch (e: Exception) {
        null
    }
}

private fun deleteFileIfExists(context: Context, fileName: String): Boolean {
    val filesCollection = MediaStore.Files.getContentUri("external")
    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND " +
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
    val selectionArgs = arrayOf(
        fileName,
        Environment.DIRECTORY_DOCUMENTS + "/AppWifi"
    )

    val deleted = context.contentResolver.query(
        filesCollection,
        arrayOf(MediaStore.MediaColumns._ID),
        selection,
        selectionArgs,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            val uri = ContentUris.withAppendedId(filesCollection, id)
            context.contentResolver.delete(uri, null, null) > 0
        } else false
    } ?: false

    if (!deleted) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(dir, fileName)
        return file.delete()
    }
    return true
}

suspend fun showToastOnMain(context: Context, message: String) {
    withContext(Dispatchers.Main) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}

fun generateDateRange(startDate: String, endDate: String): List<String> {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val start = dateFormat.parse(startDate)
    val end = dateFormat.parse(endDate)

    val dates = mutableListOf<String>()
    val calendar = Calendar.getInstance()
    calendar.time = start

    while (calendar.time <= end) {
        dates.add(dateFormat.format(calendar.time))
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }

    return dates
}

@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun SensorScreenPreview() {
    FuelBoxControlTheme {
        SensorScreenContent(
            onBack = {},
            bleConnectionState = ConnectionState.Connected,
            sensorInfoState = SensorState(
                sensor1 = SensorData("2024-01-01", "25°C", "100L", "Good"),
                sensor2 = SensorData("2024-01-01", "26°C", "150L", "Normal"),
                sensor3 = SensorData("2024-01-01", "24°C", "200L", "Good"),
                sensor4 = SensorData("2024-01-01", "27°C", "50L", "Bad"),
                acelerometro = AccelerometerData("2024-01-01", "100")
            ),
            permissionState = object : MultiplePermissionsState {
                override val allPermissionsGranted: Boolean = true
                override val permissions: List<PermissionState> =
                    emptyList()
                override val revokedPermissions: List<PermissionState> =
                    emptyList()
                override val shouldShowRationale: Boolean = false
                override fun launchMultiplePermissionRequest() {}
            },
            sensorMessage = "Test Message",
            constante1 = "Const 1",
            fecha1 = "2024-01-01",
            alerta1 = "None",
            constante2 = "Const 2",
            fecha2 = "2024-01-01",
            alerta2 = "None",
            constante3 = "Const 3",
            alerta3 = "None",
            constante4 = "Const 4",
            fecha4 = "2024-01-01",
            alerta4 = "None",
            bateria = "80%",
            initializingMessage = null,
            errorMessage = null,
            sensoruno = "Sensor 01",
            sendingState = "Idle",
            onDisconnect = {},
            onReconnect = {},
            onInitializeConnection = {},
            onWritesDataHostPost = { _, _, _ -> },
            onWrite = {},
            onWriteEinc = {},
            onWriteRTC = {},
            onWriteEemo = {},
            onWriteElmo = {},
            onWriteSace = {},
            onClearValores = {},
            onSendRead = { _, _, _, _ -> }
        )
    }
}