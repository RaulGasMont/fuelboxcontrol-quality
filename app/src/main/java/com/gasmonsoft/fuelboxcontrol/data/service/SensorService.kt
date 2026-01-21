package com.gasmonsoft.fuelboxcontrol.data.service

import com.gasmonsoft.fuelboxcontrol.model.SensorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject

class SensorService @Inject constructor(private val sensorClient: SensorService){

   suspend fun doSensor(){
    }
}