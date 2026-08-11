package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_activities")
data class MaintenanceActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleName: String,
    val title: String, // e.g., "Oil Change", "Spark Plug Swap", "Timing Belt Repair"
    val type: String, // "MAINTENANCE" or "REPAIR"
    val status: String, // "COMPLETED" or "PLANNED" (future)
    val dateEpochMs: Long,
    val cost: Double,
    val notes: String = "",
    val mileage: Int = 0,
    val intervalMiles: Int = 0, // e.g., repeat every 300 miles
    val intervalDays: Int = 0   // e.g., repeat every 30 days
)
