package com.gasmonsoft.fuelboxcontrol.data.repository.user

import com.gasmonsoft.fuelboxcontrol.data.model.database.UserEntity
import com.gasmonsoft.fuelboxcontrol.data.model.login.LoginDto
import com.gasmonsoft.fuelboxcontrol.data.model.selectvehicle.ContenedoresAMedirResponse
import com.gasmonsoft.fuelboxcontrol.data.service.user.UserLocalDataSource
import com.gasmonsoft.fuelboxcontrol.data.service.user.UserRemoteDataSource
import com.gasmonsoft.fuelboxcontrol.utils.LBEncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) {

    fun getUser(): Flow<UserEntity?> = localDataSource.getUser()

    suspend fun login(user: String, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // El DTO espera (fld_password, fld_usuario, fld_id)
                val responseList =
                    remoteDataSource.login(LoginDto(password, user, "0")).getOrThrow()

                // Operaciones de base de datos
                localDataSource.deleteUser()
                localDataSource.upsertUser(
                    UserEntity(
                        user = responseList.fld_usuario,
                        token = responseList.token,
                        timestamp = System.currentTimeMillis(),
                        idEmpresa = responseList.id_empresa,
                        name = responseList.nombreCompleto,
                        cajasCalidad = responseList.cajasCalidad
                    )
                )
            }
        }

    suspend fun deleteUser(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { localDataSource.deleteUser() }
        }
}
