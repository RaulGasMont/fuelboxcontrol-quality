package com.gasmonsoft.fbccalidad.data.repository.container

import com.gasmonsoft.fbccalidad.data.datasource.container.LocalContainerDataSource
import com.gasmonsoft.fbccalidad.data.datasource.container.RemoteContainerDataSource
import com.gasmonsoft.fbccalidad.data.model.selectvehicle.toEntity
import com.gasmonsoft.fbccalidad.data.service.user.UserLocalDataSource
import com.gasmonsoft.fbccalidad.utils.LBEncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContainerRepository @Inject constructor(
    private val localContainerDataSource: LocalContainerDataSource,
    private val localUserDataSource: UserLocalDataSource,
    private val remoteContainerDataSource: RemoteContainerDataSource
) {
    suspend fun getContainers() = withContext(Dispatchers.IO) {
        runCatching {
            val containers = localContainerDataSource.getContainers().first()
            if (containers.isEmpty()) {
                saveAllContainers().getOrThrow()
            }
            localContainerDataSource.getContainers()
        }
    }

    suspend fun saveAllContainers() = withContext(Dispatchers.IO) {
        runCatching {
            val user = localUserDataSource.getUser().first()
                ?: throw Exception("Usuario no encontrado.")
            val encryptedIdEmpresa = LBEncryptionUtils.encrypt(user.idEmpresa.toString())
            val result = remoteContainerDataSource.getContainers(
                idEmpresa = encryptedIdEmpresa,
                token = user.token
            ).getOrThrow().map { it.toEntity() }
            localContainerDataSource.saveAllContainers(result)
        }
    }

    suspend fun deleteContainer() = withContext(Dispatchers.IO) {
        runCatching {
            localContainerDataSource.deleteAllContainers()
        }
    }
}