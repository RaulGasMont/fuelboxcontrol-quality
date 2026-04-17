package com.gasmonsoft.fbccalidad.ui.login.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fbccalidad.R
import com.gasmonsoft.fbccalidad.ui.commons.ErrorDialog
import com.gasmonsoft.fbccalidad.ui.commons.LoadingDialog
import com.gasmonsoft.fbccalidad.ui.login.viewmodel.LoginViewModel
import com.gasmonsoft.fbccalidad.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fbccalidad.utils.ProcessingEvent


@Composable
fun LoginRoute(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.loginEvent) {
        if (uiState.loginEvent is ProcessingEvent.Success) {
            onHome()
        }
    }

    when (uiState.loginEvent) {
        is ProcessingEvent.Error -> {
            ErrorDialog(
                message = "No se pudo iniciar sesión. Intente de nuevo.",
                onDismiss = viewModel::dismissLoginError
            )
        }

        is ProcessingEvent.Loading -> LoadingDialog()
        else -> Unit
    }

    LoginScreen(
        isLoading = uiState.loginEvent is ProcessingEvent.Loading,
        onLogin = viewModel::login,
        modifier = modifier
    )
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onLogin: (user: String, pin: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var user by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var pinVisible by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val isFormValid = user.isNotBlank() && pin.isNotBlank()

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 430.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 14.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(88.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.fbc_icon),
                                    contentDescription = "FuelBoxControl logo",
                                    modifier = Modifier.size(54.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "FuelBoxControl",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Control de calidad de combustible",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Iniciar sesión",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Accede con tus credenciales para continuar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        OutlinedTextField(
                            value = user,
                            onValueChange = { user = it },
                            label = { Text("Usuario") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null
                                )
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("Pin") },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { pinVisible = !pinVisible }) {
                                    Icon(
                                        imageVector = if (pinVisible) {
                                            Icons.Outlined.VisibilityOff
                                        } else {
                                            Icons.Outlined.Visibility
                                        },
                                        contentDescription = if (pinVisible) {
                                            "Ocultar pin"
                                        } else {
                                            "Mostrar pin"
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (pinVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (isFormValid && !isLoading) {
                                        onLogin(user.trim(), pin.trim())
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(26.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onLogin(user.trim(), pin.trim())
                            },
                            enabled = isFormValid && !isLoading,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Iniciar sesión",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    FuelBoxControlTheme {
        LoginScreen(
            isLoading = false,
            onLogin = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenLoadingPreview() {
    FuelBoxControlTheme {
        LoginScreen(
            isLoading = true,
            onLogin = { _, _ -> }
        )
    }
}
