package com.gasmonsoft.fuelboxcontrol.ui.calibracion.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.gasmonsoft.fuelboxcontrol.data.ble.SensorReceiveManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val sensorReceiveManager: SensorReceiveManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

}