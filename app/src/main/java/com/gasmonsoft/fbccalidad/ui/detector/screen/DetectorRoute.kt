package com.gasmonsoft.fbccalidad.ui.detector.screen

import android.hardware.usb.UsbDevice
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.gasmonsoft.fbccalidad.R
import com.gasmonsoft.fbccalidad.domain.model.QualityRange
import com.gasmonsoft.fbccalidad.ui.commons.ErrorDialog
import com.gasmonsoft.fbccalidad.ui.commons.LoadingDialog
import com.gasmonsoft.fbccalidad.ui.detector.viewmodel.DetectorChannelType
import com.gasmonsoft.fbccalidad.ui.detector.viewmodel.DetectorUiState
import com.gasmonsoft.fbccalidad.ui.detector.viewmodel.DetectorViewModel
import com.gasmonsoft.fbccalidad.ui.detector.viewmodel.TransferState
import com.gasmonsoft.fbccalidad.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fbccalidad.utils.LoadState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DetectorRoute(
    modifier: Modifier = Modifier,
    onSelectTank: () -> Unit,
    viewModel: DetectorViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()

    DetectorScreen(
        uiState = uiState.value,
        modifier = modifier,
        onSelectTank = onSelectTank,
        onAnalyze = { viewModel.analyzeData(it) },
        onDismissDetection = { viewModel.updateDetectionEvent(LoadState.Idle) },
        onPermissionRequest = viewModel::onPermissionRequest,
        onRefreshDetection = viewModel::refreshDetection,
        onAcceptMatterLoadError = viewModel::acceptMatterLoadError,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetectorScreen(
    uiState: DetectorUiState,
    modifier: Modifier = Modifier,
    onSelectTank: () -> Unit,
    onAnalyze: (channel: DetectorChannelType) -> Unit,
    onDismissDetection: () -> Unit,
    onPermissionRequest: (device: UsbDevice) -> Unit,
    onRefreshDetection: () -> Unit,
    onAcceptMatterLoadError: () -> Unit
) {
    var showChannelDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fuelAnalyseRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(showChannelDialog) {
        if (showChannelDialog) {
            onRefreshDetection()
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        )
    )

    if (showChannelDialog) {
        DetectorChannelDialog(
            onDismiss = { showChannelDialog = false },
            onChannelSelected = { channel ->
                if (channel == DetectorChannelType.USB) {
                    val isOtgReady = uiState.otgTransferState is TransferState.DeviceReady
                    if (isOtgReady) onAnalyze(channel)
                    else if (uiState.otgTransferState is TransferState.DeviceDetected) {
                        // Se dispara el diálogo de permiso
                        uiState.otgTransferState.device.let { device ->
                            onPermissionRequest(device)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "La caja de comunicaciones no se encuentra disponible (USB).",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    onAnalyze(channel)
                }

                showChannelDialog = false
            }
        )
    }

    LaunchedEffect(uiState.detectionEvent) {
        if (uiState.detectionEvent is LoadState.Success) {
            coroutineScope.launch {
                fuelAnalyseRequester.bringIntoView()
            }
            onDismissDetection()
        }
    }

    when (uiState.detectionEvent) {
        is LoadState.Loading -> {
            LoadingDialog(
                title = "Analizando",
                message = "Por favor espere mientras se analizan los datos..."
            )
        }

        is LoadState.Error -> {
            ErrorDialog(
                message = "Ocurrió un error al analizar los datos. Intente de nuevo.",
                onDismiss = onDismissDetection
            )
        }

        else -> Unit
    }

    val canAnalyze =
        uiState.tankId != -1 && uiState.detectionEvent !is LoadState.Loading
    val levelPercent = (uiState.valueDetection.coerceIn(0f, 1f) * 100f).roundToInt()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .imePadding(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            showChannelDialog = true
                        },
                        enabled = canAnalyze,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.size(10.dp))

                        Text(
                            text = "Analizar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DetectorHeaderCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                )

                when (uiState.loadScreen) {
                    is LoadState.Loading -> {
                        LoadingDialog(message = "Por favor espere mientras se cargan las sustancias...")
                    }

                    is LoadState.Success -> {
                        TankSelectionCard(
                            tankId = uiState.tankId,
                            tankName = uiState.tankName,
                            onSelectTank = onSelectTank,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        )

                        AnalysisSummaryCard(
                            isLoading = uiState.detectionEvent == LoadState.Loading,
                            fuelType = uiState.fuelType,
                            rawValue = uiState.valueDetection,
                            levelPercent = levelPercent,
                            bringIntoViewRequester = fuelAnalyseRequester,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 520.dp)
                        )
                    }

                    is LoadState.Error -> {
                        ErrorDialog(
                            message = "No se pudieron cargar las sustancias. Se cargaran las muestras por defecto.",
                            onDismiss = onAcceptMatterLoadError
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DetectorHeaderCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.fbc_icon),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Detector de calidad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Analice el contenido del tanque seleccionado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TankSelectionCard(
    tankId: Int,
    tankName: String,
    onSelectTank: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Tanque",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Seleccione la unidad que desea analizar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                onClick = onSelectTank,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalGasStation,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.size(10.dp))

                Text(
                    text = if (tankId == -1) "Seleccionar tanque" else "Cambiar tanque",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (tankId != -1) {
                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Unidad seleccionada",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = tankName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnalysisSummaryCard(
    isLoading: Boolean,
    fuelType: QualityRange?,
    rawValue: Float,
    levelPercent: Int,
    bringIntoViewRequester: BringIntoViewRequester,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Resumen actual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (!isLoading) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    shape = CircleShape,
                    color = Color(0xFF0F172A).copy(alpha = 0.72f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (!isLoading) "Sustancia: ${fuelType?.label ?: "Sin análisis"}" else "Analizando...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .bringIntoViewRequester(bringIntoViewRequester),
                contentAlignment = Alignment.Center
            ) {
                AnimatedFuelTank(
                    fuelType = fuelType,
                    level = 100f,
                    modifier = Modifier.size(width = 220.dp, height = 340.dp),
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentHeight(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, name = "Resultado Adulterado")
@Composable
fun DetectorSuccessAdulteratedPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(
                tankId = 1,
                tankName = "Tanque 02",
                fuelType = QualityRange.ADULTERADO,
                valueDetection = -0.5f,
                loadScreen = LoadState.Success
            ),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {},
            onPermissionRequest = {},
            onRefreshDetection = {},
            onAcceptMatterLoadError = {}
        )
    }
}

@Preview(showBackground = true, name = "Cargando Sustancias")
@Composable
fun DetectorLoadingPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(loadScreen = LoadState.Loading),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {},
            onPermissionRequest = {},
            onRefreshDetection = {},
            onAcceptMatterLoadError = {}
        )
    }
}

@Preview(showBackground = true, name = "Sin Tanque Seleccionado")
@Composable
fun DetectorNoTankPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(loadScreen = LoadState.Success),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {},
            onPermissionRequest = {},
            onRefreshDetection = {},
            onAcceptMatterLoadError = {}
        )
    }
}

@Preview(showBackground = true, name = "Listo para Analizar")
@Composable
fun DetectorReadyPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(
                tankId = 1,
                tankName = "Tanque 01",
                loadScreen = LoadState.Success
            ),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {},
            onPermissionRequest = {},
            onRefreshDetection = {},
            onAcceptMatterLoadError = {}
        )
    }
}

@Preview(showBackground = true, name = "Resultado Diesel")
@Composable
fun DetectorSuccessDieselPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(
                tankId = 1,
                tankName = "Tanque Diesel 01",
                fuelType = QualityRange.DIESEL,
                valueDetection = 2.4f,
                loadScreen = LoadState.Success
            ),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {},
            onPermissionRequest = {},
            onRefreshDetection = {},
            onAcceptMatterLoadError = {}
        )
    }
}

@Preview(showBackground = true, name = "Error de Análisis")
@Composable
fun DetectorErrorPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(
                tankId = 1,
                tankName = "Tanque 01",
                detectionEvent = LoadState.Error,
                loadScreen = LoadState.Success
            ),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {},
            onPermissionRequest = {},
            onRefreshDetection = {},
            onAcceptMatterLoadError = {}
        )
    }
}

@Preview(showBackground = true, name = "Error Carga Sustancias")
@Composable
fun DetectorMatterLoadErrorPreview() {
    FuelBoxControlTheme {
        DetectorScreen(
            uiState = DetectorUiState(
                loadScreen = LoadState.Error
            ),
            onSelectTank = {},
            onAnalyze = {},
            onDismissDetection = {},
            onPermissionRequest = {},
            onRefreshDetection = {},
            onAcceptMatterLoadError = {}
        )
    }
}