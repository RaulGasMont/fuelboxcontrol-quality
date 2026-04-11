package com.gasmonsoft.fuelboxcontrol.ui.mainnav.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.repository.user.UserRepository
import com.gasmonsoft.fuelboxcontrol.domain.session.SessionUseCase
import com.gasmonsoft.fuelboxcontrol.utils.ProcessingEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FbcState(
    val sessionExpired: Boolean = false,
    val sessionProcessEvent: ProcessingEvent = ProcessingEvent.Idle
)

@HiltViewModel
class FbcViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionUseCase: SessionUseCase
) : ViewModel() {
    private var uiState = MutableStateFlow(FbcState())
    val state = uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getUser().collect { user ->
                sessionUseCase(user).fold(
                    onSuccess = {
                        uiState.update { it.copy(sessionExpired = false) }
                    },
                    onFailure = {
                        uiState.update { it.copy(sessionExpired = true) }
                    }
                )
            }
        }
    }
}