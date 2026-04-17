package com.gasmonsoft.fbccalidad.data.service.user

import com.gasmonsoft.fbccalidad.data.dao.UserDao
import com.gasmonsoft.fbccalidad.data.model.database.UserEntity
import javax.inject.Inject

class UserLocalDataSource @Inject constructor(private val userDao: UserDao) {
    fun getUser() = userDao.getUser()
    suspend fun upsertUser(user: UserEntity) = userDao.upsertUser(user)
    suspend fun deleteUser() = userDao.deleteUser()
}