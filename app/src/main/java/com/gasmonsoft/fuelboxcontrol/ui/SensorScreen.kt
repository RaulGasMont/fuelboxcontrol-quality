package com.gasmonsoft.fuelboxcontrol.ui

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.data.ble.ConnectionState
import com.gasmonsoft.fuelboxcontrol.utils.dataprocessing.EncriptarBin
import com.gasmonsoft.fuelboxcontrol.utils.dataprocessing.ReducirDatosSensor
import com.gasmonsoft.fuelboxcontrol.utils.dataprocessing.readFile
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SensorScreen(
    nameWifi: String,
    passWifi: String,
//    onBluetoothStateChanged: () -> Unit,
    viewModelSensor: SensorViewModel = hiltViewModel()
) {

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

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
//                onBluetoothStateChanged()
            }
        }
    }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }


    val lifecycleOwner = LocalLifecycleOwner.current
    val bleConnectionState = viewModelSensor.connectionState.collectAsState().value
    val sensorMessage: String by viewModelSensor.Message.observeAsState(initial = "")

    val volumen1: String by viewModelSensor.volumen1.observeAsState(initial = "")
    val temperatura1: String by viewModelSensor.temperatura1.observeAsState(initial = "")
    val constante1: String by viewModelSensor.constante1.observeAsState("")
    val fecha1: String by viewModelSensor.fecha1.observeAsState("")
    val alerta1: String by viewModelSensor.alerta1.observeAsState("")

    val volumen2: String by viewModelSensor.volumen2.observeAsState("")
    val temperatura2: String by viewModelSensor.temperatura2.observeAsState("")
    val constante2: String by viewModelSensor.constante2.observeAsState("")
    val fecha2: String by viewModelSensor.fecha2.observeAsState("")
    val alerta2: String by viewModelSensor.alerta2.observeAsState("")

    val volumen3: String by viewModelSensor.volumen3.observeAsState("")
    val temperatura3: String by viewModelSensor.temperatura3.observeAsState("")
    val constante3: String by viewModelSensor.constante3.observeAsState("")
    val fecha3: String by viewModelSensor.fecha3.observeAsState("")
    val alerta3: String by viewModelSensor.alerta3.observeAsState("")
    val volumen4: String by viewModelSensor.volumen4.observeAsState("")
    val temperatura4: String by viewModelSensor.temperatura4.observeAsState("")
    val constante4: String by viewModelSensor.constante4.observeAsState("")
    val fecha4: String by viewModelSensor.fecha4.observeAsState("")
    val alerta4: String by viewModelSensor.alerta4.observeAsState("")
    val senial: String by viewModelSensor.senial.observeAsState("")
    val bateria: String by viewModelSensor.bateria.observeAsState("")
    var isLoading by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    var textValue by remember { mutableStateOf("") }
    var textValueEemo by remember { mutableStateOf("") }
    var textValueElmo by remember { mutableStateOf("") }
    var textValueSACE by remember { mutableStateOf("") }
    var textValueRTC by remember { mutableStateOf("") }
    var textValueEINC by remember { mutableStateOf("") }
    //puntowifi
    val ssid = remember { mutableStateOf(nameWifi) }
    val password = remember { mutableStateOf(passWifi) }
    val isWifiEnabled = remember { mutableStateOf(false) }
    val connectionState = viewModelSensor.connectionStates.collectAsState()
    val sendingState = viewModelSensor.sendingState.collectAsState()
    val discoveredServices = viewModelSensor.discoveredServices.collectAsState()
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
                        viewModelSensor.reconnect()
                    }
                }
                if (event == Lifecycle.Event.ON_STOP) {
                    if (bleConnectionState == ConnectionState.Connected) {
                        viewModelSensor.disconnect()
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
                viewModelSensor.initializeConnection()
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .wrapContentSize()
            .background(Color.White)
            .padding(0.dp)
    ) {


        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.DarkGray), // Ocupa todo el ancho de la columna
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "------CONEXIÓN AUTOMÁTICA BLE-------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {

            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bleConnectionState == ConnectionState.CurrentlyInitializing) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .wrapContentSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            if (viewModelSensor.initializingMessage != null) {
                                Text(
                                    text = viewModelSensor.initializingMessage!!,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Magenta
                                )
                            }
                        }

                    } else if (!permissionState.allPermissionsGranted) {
                        Text(
                            text = "Vaya a la configuración de la aplicación y permita los permisos que faltan.",
                            modifier = Modifier.padding(2.dp),
                            textAlign = TextAlign.Center
                        )
                    } else if (viewModelSensor.errorMessage != null) {

                        Text(
                            text = sensorMessage,
                            style = TextStyle(
                                color = Color(R.color.purple_500),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Column(
                            modifier = Modifier
                                .wrapContentSize(),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                style = TextStyle(
                                    color = Color(R.color.purple_500),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                text = viewModelSensor.errorMessage!!
                            )
                            Button(
                                onClick = {
                                    if (permissionState.allPermissionsGranted) {
                                        viewModelSensor.initializeConnection()
                                    }
                                }, shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Intentar otra vez"
                                )
                            }
                        }
                    } else if (bleConnectionState == ConnectionState.Connected) {
                        Column(
                            modifier = Modifier
                                .wrapContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = "  ${viewModelSensor.sensoruno}",
                                style = TextStyle(
                                    color = Color(R.color.purple_500),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                // style = MaterialTheme.typography.h6
                            )
                            Button(
                                onClick = {
                                    if (permissionState.allPermissionsGranted) {
                                        viewModelSensor.disconnect()
                                    }
                                }, modifier = Modifier
                                    .wrapContentSize()
                                    .padding(10.dp)
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    contentColor = Color(R.color.purple_500),
                                    disabledContentColor = Color(R.color.purple_500),
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Desconectar", style = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }


                        }
                    } else if (bleConnectionState == ConnectionState.Disconnected) {
                        Button(
                            onClick = {
                                viewModelSensor.initializeConnection()
                            },
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(20.dp)
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                contentColor = Color(R.color.purple_500),
                                disabledContentColor = Color(R.color.purple_500),
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Inicializar de nuevo", style = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        val context = LocalContext.current

                        Button(
                            onClick = {
//                                viewModelSensor.omemvio(context)
                            }, modifier = Modifier
                                .wrapContentSize()
                                .padding(20.dp)
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                contentColor = Color(R.color.purple_500),
                                disabledContentColor = Color(R.color.purple_500),
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Enviar Datos", style = TextStyle(
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }


                    }
                }
            }

        }


        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "------CREAR PUNTO DE ACCESO-------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {

            Column(
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                OutlinedTextField(
                    value = ssid.value,
                    onValueChange = { if (it.length <= 4) ssid.value = it },
                    label = { Text("SSID (4 caracteres)") },
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { if (it.length <= 8) password.value = it },
                    label = { Text("Contraseña (8 caracteres)") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text("Activar:")
                    Switch(
                        checked = isWifiEnabled.value,
                        onCheckedChange = { isWifiEnabled.value = it }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top,

                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            viewModelSensor.writesDataHostPost(
                                ssid.value,
                                password.value,
                                isWifiEnabled.value
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Text("Crear HostPost")
                    }
                }
                Text(text = sendingState.value)
                Spacer(modifier = Modifier.height(16.dp))


                Column(
                    modifier = Modifier.fillMaxWidth()
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
                        onClick = { viewModelSensor.disconnect() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        Text("Desconectar Bluetooth")

                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }


        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "---------ENVÍO INFORMACIÓN HOSTPOST-----",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            TextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("IP Address") },
                modifier = Modifier.fillMaxWidth().padding(6.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth().padding(6.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Mensaje") }, modifier = Modifier.fillMaxWidth().padding(6.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    connectToEsp32(ip, port, message, context, { isConnected = it })
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Conectar Wifi y enviar MSG")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "------VALOR RST--------",
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho disponible
                textAlign = TextAlign.Center
            )
            TextField(
                value = messagestatus,
                onValueChange = { messagestatus = it },
                label = { Text("Valor") }, modifier = Modifier.fillMaxWidth().padding(6.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {

                    sendValorToESP32(ip, port, context, messagestatus)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Enviar valor rst")
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text(text = if (isConnected) "Conectado" else "No conectado")

            Text(
                text = "------LÍMITE--------",
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho disponible
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))


            TextField(
                value = limit,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } && newValue.length <= 1) {
                        limit = newValue
                    }
                },
                label = { Text("Límite (1-4)") },
                modifier = Modifier.fillMaxWidth().padding(6.dp)
            )

            Button(
                onClick = { sendLimits(ip, port, limit.toString(), context) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Enviar archivo límites")
            }


            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "------CONFIGURACIÓN--------",
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho disponible
                textAlign = TextAlign.Center
            )

            Button(onClick = { launcher.launch("application/json") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
                    .padding(4.dp)) {
                Text("Seleccionar archivo JSON")
            }




            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = jsonConfig,
                onValueChange = {},
                readOnly = true,
                label = { Text("Contenido del archivo JSON") },
                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            )



            Button(
                onClick = { sendConfigToESP32(ip, port, context, jsonConfig) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Enviar configuración")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "------SEND actFw--------",
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Button(onClick = { launcheract.launch(arrayOf("text/x-python", "text/mpy")) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
                    .padding(4.dp)) {
                Text("Seleccionar archivo")
            }



            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = act,
                onValueChange = {},
                readOnly = true,
                label = { Text("Contenido del archivo ") },
                textStyle = LocalTextStyle.current.copy(fontSize = 10.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            )



            Button(
                onClick = { sendaCTToESP32(ip, port, context, act, fileNameWithExtension) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Enviar actFW")
            }


            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "------LECTURA--------",fontWeight = FontWeight.Bold,
                color = Color.Red,modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center)
            Text(
                text = "------SELECCIONAR RANGO DE FECHAS------",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = TextStyle(fontSize = 16.sp)
            )

            DateSelector("Fecha de Inicio") { date ->
                startDate = date
            }

            Spacer(modifier = Modifier.height(16.dp))


            DateSelector("Fecha de Fin") { date ->
                endDate = date
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = filePath,
                onValueChange = { filePath = it },
                label = { Text("Ruta de archivo para leer") }

                ,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                        coroutineScope.launch {
                            try {

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Procesando archivos...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }


                                val dateRange = generateDateRange(startDate, endDate)
                                val processedFiles = mutableListOf<Uri>()

                                delay(1000)
                                viewModelSensor.onwrite("1")
                                dateRange.forEachIndexed { index, date ->
                                    filePath = "/sd/muestras/$date.bin"
                                    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
                                    val fecha = LocalDate.parse(date, formatter)

                                    val dia = fecha.dayOfMonth.toString()
                                    val uri = readFile(ip, port, filePath, "${date}.bin", context,dia)

                                    uri?.let {
                                        processedFiles.add(it)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Procesado ${index + 1} de ${dateRange.size}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }


                                withContext(Dispatchers.Main) {
                                    if (processedFiles.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            "No se encontraron archivos en el rango de fechas",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@withContext
                                    }

                                    Toast.makeText(
                                        context,
                                        "${processedFiles.size} archivos listos para enviar",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }


                                delay(2000)


                                processedFiles.forEachIndexed { index, uri ->
                                    sendValorToESP32(ip, port, context, "1")
                                    delay(2000)
                                    viewModelSensor.onSendRead(context, uri,"2025/03/02","1")

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Enviando archivo ${index + 1}/${processedFiles.size}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    delay(500)
                                }


                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "✅ ${processedFiles.size} archivos enviados correctamente",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "❌ Error: ${e.localizedMessage ?: "Falló el proceso"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Seleccione ambas fechas primero",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Leer y enviar archivos")
            }



            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "------DEL--------",
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            TextField(
                value = filePathDel,
                onValueChange = { filePathDel = it },
                label = { Text("Ruta de archivo DEL") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))


            Button(
                onClick = { sendDelToESP32(ip, port, context, filePathDel) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Enviar DEL")
            }




            Spacer(modifier = Modifier.height(8.dp))




        }



        item {
            Column(
                modifier = Modifier.fillMaxWidth()
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
                onClick = { viewModelSensor.onwriteEinc(textValueEINC) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(4.dp)
            ) {
                Text("Enviar EINC")
            }
        }
//// RTC

        item {
            Column(
                modifier = Modifier.fillMaxWidth()
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
            Row(
                modifier = androidx.compose.ui.Modifier
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
                        viewModelSensor.onwriteRTC(enteredText)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color(R.color.purple_500),
                        disabledContentColor = Color(R.color.purple_500),
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
                modifier = Modifier.fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "------------ENVÍO COMM------------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {
            Row(
                modifier = androidx.compose.ui.Modifier
                    .padding(2.dp)
                    .wrapContentSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


                TextField(
                    value = textValue,

                    onValueChange = {
                        textValue = it
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                )

                Button(
                    onClick = {
                        val enteredText = textValue
                        viewModelSensor.onwrite(enteredText)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color(R.color.purple_500),
                        disabledContentColor = Color(R.color.purple_500),
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Enviar COMM", style = TextStyle(
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
                modifier = Modifier.fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "------------ENVÍO EEMO------------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {
            Row(
                modifier = androidx.compose.ui.Modifier
                    .padding(2.dp)
                    .wrapContentSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


                TextField(
                    value = textValueEemo,

                    onValueChange = {
                        textValueEemo = it
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                )

                Button(
                    onClick = {
                        val enteredText = textValueEemo
                        viewModelSensor.onwriteEemo(enteredText)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color(R.color.purple_500),
                        disabledContentColor = Color(R.color.purple_500),
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Enviar EEMO", style = TextStyle(
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
                modifier = Modifier.fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "------------ENVÍO ELMO------------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {
            Row(
                modifier = androidx.compose.ui.Modifier
                    .padding(2.dp)
                    .wrapContentSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // TextField

                TextField(
                    value = textValueElmo,

                    onValueChange = {

                        textValueElmo = it
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                )

                Button(
                    onClick = {
                        val enteredText = textValueElmo
                        viewModelSensor.onwriteElmo(enteredText)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color(R.color.purple_500),
                        disabledContentColor = Color(R.color.purple_500),
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Enviar ELMO", style = TextStyle(
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
                modifier = Modifier.fillMaxWidth()
                    .background(Color.DarkGray),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "------------ENVÍO SACE------------",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        item {
            Row(
                modifier = androidx.compose.ui.Modifier
                    .padding(2.dp)
                    .wrapContentSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {


                TextField(
                    value = textValueSACE,

                    onValueChange = {
                        textValueSACE = it
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                )

                Button(
                    onClick = {

                        val enteredText = textValueSACE
                        viewModelSensor.onwritesace(enteredText)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color(R.color.purple_500),
                        disabledContentColor = Color(R.color.purple_500),
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Enviar SACE", style = TextStyle(
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
                modifier = Modifier.fillMaxWidth()
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
            Text(
                text = volumen1,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = temperatura1,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = constante1,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }


        item {
            Text(
                text = alerta1,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = volumen2,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = temperatura2,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }
        item {
            Text(
                text = constante2,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = alerta2,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }
        item {
            Text(
                text = volumen3,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = temperatura3,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = constante3,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }


        item {
            Text(
                text = alerta3,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }
        item {

            Text(
                text = volumen4,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }
        item {
            Text(
                text = temperatura4,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = constante4,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }


        item {
            Text(
                text = alerta4,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        item {
            Text(
                text = fecha1,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        item {
            Text(
                text = fecha4,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }


        item {

            Text(
                text = fecha2,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

        }

        item {
            Text(
                text = bateria,
                style = TextStyle(
                    color = Color(R.color.gray),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )


            Text(
                text = sensorMessage,
                style = TextStyle(
                    color = Color(R.color.purple_500),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        item {
            Button(
                onClick = {
                    viewModelSensor.clearvalores()
                },


                colors = ButtonDefaults.buttonColors(
                    contentColor = Color(R.color.purple_500),
                    disabledContentColor = Color(R.color.purple_500),
                ),
                shape = RoundedCornerShape(8.dp)

            ) {
                Text(
                    text = "Limpiar", style = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
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
fun DateSelector(label: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    var selectedDate by remember { mutableStateOf("") }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = "$selectedYear/${selectedMonth + 1}/$selectedDay"
            selectedDate = formattedDate
            onDateSelected(formattedDate)
        },
        year,
        month,
        day
    )

    TextField(
        value = selectedDate,
        onValueChange = {},
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .clickable { datePickerDialog.show() },
        readOnly = true,
        enabled = false
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AppWifi")
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
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AppWifi")
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