package com.gasmonsoft.fuelboxcontrol.data.repository.user

import com.gasmonsoft.fuelboxcontrol.data.model.database.UserEntity
import com.gasmonsoft.fuelboxcontrol.data.model.login.LoginDto
import com.gasmonsoft.fuelboxcontrol.data.service.user.UserLocalDataSource
import com.gasmonsoft.fuelboxcontrol.data.service.user.UserRemoteDataSource
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource
) {
//    fun getUser(): Flow<UserEntity> {
//        return localDataSource.getUserById()
//            .map<UserEntity?, UserUiState> { user ->
//                UserUiState.Success(user)
//            }
//            .onStart {
//                emit(UserUiState.Loading)
//            }
//            .catch { e ->
//                emit(UserUiState.Error(e.message ?: "Error desconocido"))
//            }
//    }

    suspend fun saveUser(user: String, password: String): Result<Unit> {
        return try {
            remoteDataSource.login(
                user = LoginDto(
                    username = user,
                    password = password,
                    fbToken = ""
                )
            ).fold(
                onSuccess = {
                    localDataSource.upsertUser(
                        user = UserEntity(
                            name = it.first().fld_usuario,
                            token = it.first().token
                        )
                    )
                    Result.success(Unit)
                },
                onFailure = {
                    Result.failure(it)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(): Result<Unit> {
        return try {
            localDataSource.deleteUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}