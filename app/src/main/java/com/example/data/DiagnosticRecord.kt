package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_records")
data class DiagnosticRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val soundFrequencyData: String, // e.g. "Peak Frequencies: 120Hz, 340Hz, Avg dB: 78"
    val healthStatus: String, // AI-generated or acoustic health status
    val healthScore: Int = 85,
    val aiDiagnosticSummary: String = "",
    val errorDetails: String? = null
)
