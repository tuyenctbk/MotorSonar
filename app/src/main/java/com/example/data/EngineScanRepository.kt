package com.example.data

import kotlinx.coroutines.flow.Flow

class EngineScanRepository(
    private val engineScanDao: EngineScanDao,
    private val maintenanceActivityDao: MaintenanceActivityDao,
    private val diagnosticRecordDao: DiagnosticRecordDao? = null
) {
    val allScans: Flow<List<EngineScan>> = engineScanDao.getAllScans()

    fun getScansForVehicle(vehicleName: String): Flow<List<EngineScan>> {
        return engineScanDao.getScansForVehicle(vehicleName)
    }

    suspend fun getBaselineForVehicle(vehicleName: String): EngineScan? {
        return engineScanDao.getBaselineForVehicle(vehicleName)
    }

    suspend fun insertScan(scan: EngineScan): Long {
        return engineScanDao.insertScan(scan)
    }

    suspend fun updateScan(scan: EngineScan) {
        engineScanDao.insertScan(scan)
    }

    suspend fun deleteScanById(id: Int) {
        engineScanDao.deleteScanById(id)
    }

    suspend fun clearAll() {
        engineScanDao.deleteAllScans()
        maintenanceActivityDao.deleteAllActivities()
        diagnosticRecordDao?.deleteAllRecords()
    }

    // --- Diagnostic Records ---
    val allDiagnosticRecords: Flow<List<DiagnosticRecord>>? = diagnosticRecordDao?.getAllRecords()

    fun getDiagnosticRecordsForVehicle(vehicleName: String): Flow<List<DiagnosticRecord>>? {
        return diagnosticRecordDao?.getRecordsForVehicle(vehicleName)
    }

    suspend fun insertDiagnosticRecord(record: DiagnosticRecord): Long {
        return diagnosticRecordDao?.insertRecord(record) ?: -1L
    }

    // --- Maintenance & Repairs ---
    val allActivities: Flow<List<MaintenanceActivity>> = maintenanceActivityDao.getAllActivities()

    fun getActivitiesForVehicle(vehicleName: String): Flow<List<MaintenanceActivity>> {
        return maintenanceActivityDao.getActivitiesForVehicle(vehicleName)
    }

    suspend fun insertActivity(activity: MaintenanceActivity): Long {
        return maintenanceActivityDao.insertActivity(activity)
    }

    suspend fun deleteActivityById(id: Int) {
        maintenanceActivityDao.deleteActivityById(id)
    }
}

