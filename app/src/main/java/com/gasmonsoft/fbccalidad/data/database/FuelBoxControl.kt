package com.gasmonsoft.fbccalidad.data.database

import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.RoomDatabase
import com.gasmonsoft.fbccalidad.data.dao.ContainerDao
import com.gasmonsoft.fbccalidad.data.dao.UserDao
import com.gasmonsoft.fbccalidad.data.model.database.ContainerEntity
import com.gasmonsoft.fbccalidad.data.model.database.UserEntity

@Keep
@Database(entities = [UserEntity::class, ContainerEntity::class], version = 6, exportSchema = false)
abstract class FuelBoxControlDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun containerDao(): ContainerDao
}