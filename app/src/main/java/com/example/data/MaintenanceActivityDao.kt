package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceActivityDao {
    @Query("SELECT * FROM maintenance_activities ORDER BY dateEpochMs DESC")
    fun getAllActivities(): Flow<List<MaintenanceActivity>>

    @Query("SELECT * FROM maintenance_activities WHERE vehicleName = :vehicleName ORDER BY dateEpochMs DESC")
    fun getActivitiesForVehicle(vehicleName: String): Flow<List<MaintenanceActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: MaintenanceActivity): Long

    @Query("DELETE FROM maintenance_activities WHERE id = :id")
    suspend fun deleteActivityById(id: Int)

    @Query("DELETE FROM maintenance_activities")
    suspend fun deleteAllActivities()
}
