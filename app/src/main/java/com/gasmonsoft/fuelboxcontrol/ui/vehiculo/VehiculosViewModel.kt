package com.gasmonsoft.fuelboxcontrol.ui.vehiculo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehiculosViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehiculoUiState())
    val uiState: StateFlow<VehiculoUiState> = _uiState.asStateFlow()

    fun doLogin(username: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loginEvent = NetworkEvent.Loading
                )
            }

            val login = loginUseCase(
                username = username,
                password = password
            )

            login.fold(
                onSuccess = { data ->
                    _uiState.update { currentUiState ->
                        if (data != null) {
                            currentUiState.copy(
                                userData = data,
                                loginEvent = NetworkEvent.Success("Ingreso exitoso.")
                            )
                        } else {
                            currentUiState.copy(
                                loginEvent = NetworkEvent.Error("El usuario no cuenta con vehiculos.")
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            loginEvent = NetworkEvent.Error("Ocurrio un problema al traer la informacion del servidor.")
                        )
                    }
                }
            )
        }
    }

}