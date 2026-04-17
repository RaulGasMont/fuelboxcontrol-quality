package com.gasmonsoft.fbccalidad.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gasmonsoft.fbccalidad.data.model.database.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user ORDER BY id DESC LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("DELETE FROM user")
    suspend fun deleteUser()
}
