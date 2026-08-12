package com.soloprono.motorsonar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EngineScanDao {
    @Query("SELECT * FROM engine_scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<EngineScan>>

    @Query("SELECT * FROM engine_scans WHERE vehicleName = :vehicleName ORDER BY timestamp DESC")
    fun getScansForVehicle(vehicleName: String): Flow<List<EngineScan>>

    @Query("SELECT * FROM engine_scans WHERE vehicleName = :vehicleName AND isBaseline = 1 LIMIT 1")
    suspend fun getBaselineForVehicle(vehicleName: String): EngineScan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: EngineScan): Long

    @Update
    suspend fun updateScan(scan: EngineScan)

    @Query("DELETE FROM engine_scans WHERE id = :id")
    suspend fun deleteScanById(id: Int)

    @Query("DELETE FROM engine_scans")
    suspend fun deleteAllScans()
}

