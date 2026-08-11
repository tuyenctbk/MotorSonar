package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "engine_scans")
data class EngineScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vehicleName: String,
    val vehicleType: String, // "Car" or "Motorcycle/Scooter"
    val healthScore: Int, // 0 - 100
    val timestamp: Long = System.currentTimeMillis(),
    val isBaseline: Boolean = false,
    val issueName: String, // e.g., "Squeaking alternator belt", "Engine knocking"
    val issueDescription: String,
    val urgency: String, // "SAFE", "SCHEDULE_CHECK", "STOP_DRIVING"
    val repairCostEstimate: String, // e.g., "$30 - $60"
    val mechanicPhrase: String, // Technical phrase for mechanics
    val mechanicRecommendation: String,
    val audioFilePath: String? = null, // For the "Play Filtered Sound" button
    val symptomNotes: String = "", // Symptom notes entered or observed during diagnostic
    val rawAudioAnalysisSummary: String = "", // Raw audio spectrum, peak frequencies, dB, & estimated RPM summary
    val voiceNotePath: String? = null, // Voice note recorded for mechanic context
    val voiceNoteDurationMs: Long = 0L
)
