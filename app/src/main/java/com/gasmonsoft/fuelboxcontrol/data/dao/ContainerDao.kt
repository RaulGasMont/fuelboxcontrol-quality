package com.gasmonsoft.fuelboxcontrol.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gasmonsoft.fuelboxcontrol.data.model.database.ContainerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContainerDao {
    @Query("SELECT * FROM container")
    fun getContainers(): Flow<List<ContainerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllContainers(containers: List<ContainerEntity>)

    @Query("DELETE FROM container")
    suspend fun deleteAllContainers()
}