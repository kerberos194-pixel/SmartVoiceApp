package com.smarthome.voiceapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getAll(): Flow<List<DeviceEntity>>
    
    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getById(id: String): DeviceEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<DeviceEntity>)
    
    @Query("DELETE FROM devices")
    suspend fun deleteAll()
}
