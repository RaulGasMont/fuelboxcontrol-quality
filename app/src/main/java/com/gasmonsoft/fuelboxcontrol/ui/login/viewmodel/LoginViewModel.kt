package com.gasmonsoft.fuelboxcontrol.ui.login.viewmodel

import androidx.lifecycle.ViewModel
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class LoginUiState(
    val loginEvent: ProcessingEvent = ProcessingEvent.Idle
)

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {
    private var _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(user: String, pin: String) {

    }

}