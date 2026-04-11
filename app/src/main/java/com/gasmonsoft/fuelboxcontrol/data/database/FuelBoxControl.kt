package com.gasmonsoft.fuelboxcontrol.data.database

import androidx.annotation.Keep
import androidx.room.Database
import androidx.room.RoomDatabase
import com.gasmonsoft.fuelboxcontrol.data.dao.UserDao
import com.gasmonsoft.fuelboxcontrol.data.model.database.UserEntity

@Keep
@Database(entities = [UserEntity::class], version = 1, exportSchema = false)
abstract class FuelBoxControlDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}