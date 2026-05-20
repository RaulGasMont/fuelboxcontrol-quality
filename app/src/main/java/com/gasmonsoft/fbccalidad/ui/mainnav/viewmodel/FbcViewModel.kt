package com.gasmonsoft.fbccalidad.ui.mainnav.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.repository.datastore.DataStoreRepository
import com.gasmonsoft.fbccalidad.data.repository.user.UserRepository
import com.gasmonsoft.fbccalidad.domain.session.SessionUseCase
import com.gasmonsoft.fbccalidad.utils.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FbcState(
    val isCheckingSession: Boolean = true,
    val sessionExpired: Boolean = false,
    val hasBoxId: Boolean = false,
    val sessionProcessEvent: LoadState = LoadState.Idle
)

@HiltViewModel
class FbcViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionUseCase: SessionUseCase,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {
    private var _uiState = MutableStateFlow(FbcState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userRepository.getUser(),
                dataStoreRepository.selectedCaja
            ) { user, selectedCajaId ->
                if (user == null) {
                    return@combine FbcState(
                        isCheckingSession = false,
                        sessionExpired = false,
                        hasBoxId = false
                    )
                }

                val sessionResult = sessionUseCase(user)
                sessionResult.fold(
                    onSuccess = {
                        val hasBox = selectedCajaId != -1
                        FbcState(
                            isCheckingSession = false,
                            sessionExpired = false,
                            hasBoxId = hasBox
                        )
                    },
                    onFailure = {
                        FbcState(
                            isCheckingSession = false,
                            sessionExpired = true,
                            hasBoxId = false
                        )
                    }
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }
}