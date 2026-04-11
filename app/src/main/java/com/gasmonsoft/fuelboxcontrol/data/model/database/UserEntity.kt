package com.gasmonsoft.fuelboxcontrol.data.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val token: String,
    val timestamp: Long
)