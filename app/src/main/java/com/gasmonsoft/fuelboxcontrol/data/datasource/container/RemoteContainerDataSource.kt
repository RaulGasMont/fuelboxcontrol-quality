package com.gasmonsoft.fuelboxcontrol.data.datasource.container

import com.gasmonsoft.fuelboxcontrol.data.client.FuelSoftwareService
import com.gasmonsoft.fuelboxcontrol.data.service.utils.networkRequestHelper
import javax.inject.Inject

class RemoteContainerDataSource @Inject constructor(
    private val fscService: FuelSoftwareService
) {
    suspend fun getContainers(idEmpresa: String, token: String) = networkRequestHelper {
        fscService.getContenedoresAMedir(
            token = "Bearer $token",
            idEmpresa = idEmpresa
        )
    }
}