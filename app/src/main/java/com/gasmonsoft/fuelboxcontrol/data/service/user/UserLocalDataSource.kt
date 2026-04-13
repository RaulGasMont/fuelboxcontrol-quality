package com.gasmonsoft.fuelboxcontrol.data.service.user

import com.gasmonsoft.fuelboxcontrol.data.dao.UserDao
import com.gasmonsoft.fuelboxcontrol.data.model.database.UserEntity
import javax.inject.Inject

class UserLocalDataSource @Inject constructor(private val userDao: UserDao) {
    fun getUser() = userDao.getUser()
    suspend fun upsertUser(user: UserEntity) = userDao.upsertUser(user)
    suspend fun deleteUser() = userDao.deleteUser()
}