package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entities.AssetLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetLogDao {
    @Query("SELECT * FROM asset_logs WHERE assetId = :assetId ORDER BY date DESC")
    fun getLogsForAsset(assetId: String): Flow<List<AssetLogEntity>>

    @Query("SELECT * FROM asset_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<AssetLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AssetLogEntity)
}
