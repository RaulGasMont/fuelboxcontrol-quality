package com.gasmonsoft.fuelboxcontrol.data.datasource.container

import com.gasmonsoft.fuelboxcontrol.data.dao.ContainerDao
import com.gasmonsoft.fuelboxcontrol.data.model.database.ContainerEntity
import javax.inject.Inject

class LocalContainerDataSource @Inject constructor(private val containerDao: ContainerDao) {

    suspend fun getContainers() = containerDao.getContainers()

    suspend fun saveAllContainers(containers: List<ContainerEntity>) =
        containerDao.insertAllContainers(containers)

    suspend fun deleteAllContainers() = containerDao.deleteAllContainers()
}