package com.gasmonsoft.fbccalidad.data.datasource.container

import com.gasmonsoft.fbccalidad.data.dao.ContainerDao
import com.gasmonsoft.fbccalidad.data.model.database.ContainerEntity
import javax.inject.Inject

class LocalContainerDataSource @Inject constructor(private val containerDao: ContainerDao) {

    fun getContainers() = containerDao.getContainers()

    suspend fun saveAllContainers(containers: List<ContainerEntity>) =
        containerDao.insertAllContainers(containers)

    suspend fun deleteAllContainers() = containerDao.deleteAllContainers()
}