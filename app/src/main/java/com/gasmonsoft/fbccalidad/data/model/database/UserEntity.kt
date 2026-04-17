package com.gasmonsoft.fbccalidad.data.model.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user: String,
    val token: String,
    val timestamp: Long,
    val idEmpresa: Int,
    val cajasCalidad: String,
    val name: String,
)

@Entity(tableName = "container")
data class ContainerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tankId: Int,
    val tankName: String,
    val tankType: Int
)