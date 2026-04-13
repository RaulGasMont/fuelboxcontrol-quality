package com.gasmonsoft.fuelboxcontrol.domain.containers

import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.ContenedoresAMedirResponse
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Other
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.Vehicle
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.toOther
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.toVehicle
import javax.inject.Inject

data class ContenedoresResult(
    val vehicleList: List<Vehicle>,
    val othersList: List<Other>
)

class ContainersUseCase @Inject constructor() {
    operator fun invoke(list: List<ContenedoresAMedirResponse>): ContenedoresResult {
        val vehicleList = mutableListOf<Vehicle>()
        val othersList = mutableListOf<Other>()
        list.forEach {
            if (it.type == 0) {
                vehicleList.add(it.toVehicle())
            } else {
                othersList.add(it.toOther())
            }
        }
        return ContenedoresResult(vehicleList, othersList)
    }
}