package com.gasmonsoft.fbccalidad.ui.sensor.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmonsoft.fbccalidad.data.repository.api.FuelSoftwareControlRepository
import com.gasmonsoft.fbccalidad.utils.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QualityHistoryViewModel @Inject constructor(
    private val repository: FuelSoftwareControlRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QualityHistoryUiState())
    val uiState: StateFlow<QualityHistoryUiState> = _uiState.asStateFlow()

    fun loadHistory(idCajaCalidad: Int) {
        _uiState.update { it.copy(loadState = LoadState.Loading, idCajaCalidad = idCajaCalidad) }
        viewModelScope.launch {
            val result = repository.getLastQualitySensorRecords(idCajaCalidad)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        records = result.getOrNull() ?: emptyList(),
                        loadState = LoadState.Success
                    )
                }
            } else {
                _uiState.update { it.copy(loadState = LoadState.Error) }
            }
        }
    }
}
