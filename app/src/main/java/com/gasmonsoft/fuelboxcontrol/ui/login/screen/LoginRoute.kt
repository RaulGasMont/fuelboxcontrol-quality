package com.gasmonsoft.fuelboxcontrol.ui.login.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gasmonsoft.fuelboxcontrol.R
import com.gasmonsoft.fuelboxcontrol.ui.commons.ErrorDialog
import com.gasmonsoft.fuelboxcontrol.ui.commons.LoadingDialog
import com.gasmonsoft.fuelboxcontrol.ui.login.viewmodel.LoginViewModel
import com.gasmonsoft.fuelboxcontrol.ui.theme.FuelBoxControlTheme
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent

@Composable
fun LoginRoute(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()

    when (uiState.value.loginEvent) {
        is ProcessingEvent.Success -> {
            onHome()
        }

        is ProcessingEvent.Error -> {
            ErrorDialog(message = "No se pudo iniciar sesión. Intente de nuevo.") {
                viewModel.dismissLoginError()
            }
        }

        is ProcessingEvent.Loading -> {
            LoadingDialog()
        }

        else -> {}
    }

    LoginScreen(modifier = modifier, onLogin = { user, pin ->
        viewModel.login(user, pin)
    })
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLogin: (user: String, pin: String) -> Unit
) {
    var user by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Image(
                painter = painterResource(R.drawable.fbc_icon),
                contentDescription = null,
            )

            Text(
                text = "FuelBoxControl: Calidad", style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            TextField(value = user, onValueChange = { user = it }, label = {
                Text(text = "Usuario")
            })

            TextField(value = pin, onValueChange = { pin = it }, label = {
                Text("Pin")
            }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))

            Button(
                onClick = { onLogin(user, pin) },
                modifier = Modifier
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Iniciar sesión", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    FuelBoxControlTheme {
        LoginScreen(
            onLogin = { _, _ -> }
        )
    }
}