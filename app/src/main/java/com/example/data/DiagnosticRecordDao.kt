package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticRecordDao {
    @Query("SELECT * FROM diagnostic_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<DiagnosticRecord>>

    @Query("SELECT * FROM diagnostic_records WHERE vehicleName = :vehicleName ORDER BY timestamp DESC")
    fun getRecordsForVehicle(vehicleName: String): Flow<List<DiagnosticRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DiagnosticRecord): Long

    @Query("DELETE FROM diagnostic_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM diagnostic_records")
    suspend fun deleteAllRecords()
}
