package com.gasmonsoft.fbccalidad.domain.containers

import com.gasmonsoft.fbccalidad.data.model.selectvehicle.Other
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.Vehicle
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.toOther
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.toVehicle
import com.gasmonsoft.fbccalidad.data.repository.container.ContainerRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class ContenedoresResult(
    val vehicleList: List<Vehicle>,
    val othersList: List<Other>
)

class ContainersUseCase @Inject constructor(private val containerRepository: ContainerRepository) {
    suspend operator fun invoke(): Result<ContenedoresResult> {
        return containerRepository.getContainers().mapCatching { flow ->
            val containers = flow.first()
            val vehicleList = mutableListOf<Vehicle>()
            val othersList = mutableListOf<Other>()
            containers.forEach {
                if (it.tankType == 0) {
                    vehicleList.add(it.toVehicle())
                } else {
                    othersList.add(it.toOther())
                }
            }
            ContenedoresResult(vehicleList, othersList)
        }
    }
}
