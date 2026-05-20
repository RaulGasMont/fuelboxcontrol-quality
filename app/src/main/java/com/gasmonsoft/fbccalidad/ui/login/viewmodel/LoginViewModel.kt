package com.gasmonsoft.fbccalidad.ui.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.repository.user.UserRepository
import com.gasmonsoft.fbccalidad.utils.LBEncryptionUtils
import com.gasmonsoft.fbccalidad.utils.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loginEvent: LoadState = LoadState.Idle
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    private var _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(user: String, pin: String) {
        viewModelScope.launch {
            _uiState.update { currentUiState ->
                currentUiState.copy(loginEvent = LoadState.Loading)
            }

            userRepository.login(
                user = user,
                password = LBEncryptionUtils.encrypt(pin)
            ).onSuccess {
                _uiState.update { currentUiState ->
                    currentUiState.copy(loginEvent = LoadState.Success)
                }
            }.onFailure {
                _uiState.update { currentUiState ->
                    currentUiState.copy(loginEvent = LoadState.Error)
                }
            }
        }
    }

    fun dismissLoginError() {
        _uiState.update { currentUiState ->
            currentUiState.copy(loginEvent = LoadState.Idle)
        }
    }
}