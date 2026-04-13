package com.gasmonsoft.fuelboxcontrol.ui.selecttank.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fuelboxcontrol.data.repository.user.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelectTankViewModel @Inject constructor(private val userRepository: UserRepository) :
    ViewModel() {
    private val _uiState = MutableStateFlow(SelectTankUiState())
    val uiState: StateFlow<SelectTankUiState> = _uiState.asStateFlow()

    fun getTankList() {
        viewModelScope.launch {
            val containers = userRepository.getContainers()
            _uiState.update { currentUiState ->
                currentUiState.copy(
//                    tankList = containers
                )
            }
        }
    }
}