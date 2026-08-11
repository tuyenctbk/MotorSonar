package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.EngineMediaRecorder
import com.example.audio.EngineSoundProcessor
import com.example.data.EngineScan
import com.example.data.EngineScanRepository
import com.example.data.FirebaseManager
import com.example.data.MaintenanceActivity
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

import com.example.data.DiagnosticRecord
import com.example.data.GeminiDiagnosticManager
import com.example.data.GeminiResult
import com.example.data.TranslationManager
import com.example.data.UserPreferencesManager
import com.example.location.LocationManager

@OptIn(ExperimentalCoroutinesApi::class)
class EngineSoundViewModel(private val repository: EngineScanRepository) : ViewModel() {

    // User Preferences (DataStore)
    private val _unitsOfMeasurement = MutableStateFlow("Metric")
    val unitsOfMeasurement: StateFlow<String> = _unitsOfMeasurement

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _autoSyncEnabled = MutableStateFlow(true)
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled

    private val _diagnosticSensitivity = MutableStateFlow("Standard")
    val diagnosticSensitivity: StateFlow<String> = _diagnosticSensitivity

    fun initUserPreferences(context: Context) {
        val prefsManager = UserPreferencesManager.getInstance(context)
        viewModelScope.launch {
            launch { prefsManager.unitsOfMeasurement.collect { _unitsOfMeasurement.value = it } }
            launch { prefsManager.notificationsEnabled.collect { _notificationsEnabled.value = it } }
            launch { prefsManager.autoSyncEnabled.collect { _autoSyncEnabled.value = it } }
            launch { prefsManager.diagnosticSensitivity.collect { _diagnosticSensitivity.value = it } }
        }
    }

    fun setUnitsOfMeasurement(context: Context, units: String) {
        viewModelScope.launch {
            UserPreferencesManager.getInstance(context).setUnitsOfMeasurement(units)
        }
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            UserPreferencesManager.getInstance(context).setNotificationsEnabled(enabled)
        }
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            UserPreferencesManager.getInstance(context).setAutoSyncEnabled(enabled)
        }
    }

    fun setDiagnosticSensitivity(context: Context, sensitivity: String) {
        viewModelScope.launch {
            UserPreferencesManager.getInstance(context).setDiagnosticSensitivity(sensitivity)
        }
    }

    fun openNearbyMechanics(context: Context) {
        val locationManager = LocationManager(context)
        viewModelScope.launch {
            val location = locationManager.getCurrentLocation()
            locationManager.openNearbyMechanicsInMaps(
                lat = location?.latitude,
                lng = location?.longitude
            )
        }
    }

    // Scanner instance
    private val processor = EngineSoundProcessor()

    // Engine MediaRecorder instance
    private var mediaRecorderEngine: EngineMediaRecorder? = null

    private val _isMediaRecording = MutableStateFlow(false)
    val isMediaRecording: StateFlow<Boolean> = _isMediaRecording

    private val _mediaRecordDurationMs = MutableStateFlow(0L)
    val mediaRecordDurationMs: StateFlow<Long> = _mediaRecordDurationMs

    private val _mediaRecordAmplitude = MutableStateFlow(0)
    val mediaRecordAmplitude: StateFlow<Int> = _mediaRecordAmplitude

    private val _mediaRecordStatus = MutableStateFlow("Ready to Record Engine Sound")
    val mediaRecordStatus: StateFlow<String> = _mediaRecordStatus

    private val _lastRecordedFile = MutableStateFlow<File?>(null)
    val lastRecordedFile: StateFlow<File?> = _lastRecordedFile

    // Screen navigation
    private val _currentTab = MutableStateFlow("Diagnose") // "Diagnose", "Tracking", "History"
    val currentTab: StateFlow<String> = _currentTab

    // Theme Mode Preference (SYSTEM, LIGHT, DARK)
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode, context: Context? = null) {
        _themeMode.value = mode
        context?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences("motosonar_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("theme_mode", mode.name).apply()
            } catch (e: Exception) {
                Log.e("EngineSoundViewModel", "Error saving theme mode", e)
            }
        }
    }

    fun loadThemeMode(context: Context) {
        try {
            val prefs = context.getSharedPreferences("motosonar_prefs", Context.MODE_PRIVATE)
            val savedMode = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            _themeMode.value = ThemeMode.valueOf(savedMode)
        } catch (e: Exception) {
            _themeMode.value = ThemeMode.SYSTEM
        }
        
        // Initialize TranslationManager
        try {
            TranslationManager.initialize(context)
            val currentLang = TranslationManager.selectedLanguage.value
            if (currentLang != "en") {
                viewModelScope.launch {
                    TranslationManager.translateAppUI(context, currentLang)
                }
            }
        } catch (e: Exception) {
            Log.e("EngineSoundViewModel", "Error initializing TranslationManager", e)
        }
    }

    // Localization Flows and Actions
    val selectedLanguage = TranslationManager.selectedLanguage
    val isTranslatingLanguage = TranslationManager.isTranslating
    val supportedLanguages = TranslationManager.languages

    fun changeLanguage(context: Context, langCode: String) {
        TranslationManager.setLanguage(context, langCode)
        viewModelScope.launch {
            TranslationManager.translateAppUI(context, langCode)
        }
    }

    fun getTranslated(context: Context, englishText: String): String {
        return TranslationManager.getString(context, englishText)
    }

    // Vehicle Setup
    private val _vehicleName = MutableStateFlow("My Car")
    val vehicleName: StateFlow<String> = _vehicleName

    private val _vehicleType = MutableStateFlow("Car: Inline-4 Cylinder") // Detailed default type
    val vehicleType: StateFlow<String> = _vehicleType

    private val _isCustomBaselineRequested = MutableStateFlow(false)
    val isCustomBaselineRequested: StateFlow<Boolean> = _isCustomBaselineRequested

    // Real-time scan states from processor
    val isScanning = processor.isScanning
    val scanProgress = processor.scanProgress
    val amplitudeFlow = processor.amplitudeFlow
    val detectedRpm = processor.detectedRpm
    val detectedDb = processor.detectedDb
    val statusText = processor.statusText
    val isNoiseFilterEnabled = processor.isNoiseFilterEnabled
    val batterySaverMode = processor.batterySaverMode

    fun toggleNoiseFilter() {
        processor.setNoiseFilterEnabled(!processor.isNoiseFilterEnabled.value)
    }

    fun toggleBatterySaver() {
        processor.setBatterySaverMode(!processor.batterySaverMode.value)
    }

    // Selected scan for full details screen (null means show scanner or overview)
    private val _selectedScan = MutableStateFlow<EngineScan?>(null)
    val selectedScan: StateFlow<EngineScan?> = _selectedScan

    // AI Expert Insight state
    private val _aiInsight = MutableStateFlow<String?>(null)
    val aiInsight: StateFlow<String?> = _aiInsight
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading

    // All scans and vehicle specific scans
    val allScans: StateFlow<List<EngineScan>> = repository.allScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentVehicleScans: StateFlow<List<EngineScan>> = _vehicleName
        .flatMapLatest { name ->
            repository.getScansForVehicle(name)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentVehicleActivities: StateFlow<List<MaintenanceActivity>> = _vehicleName
        .flatMapLatest { name ->
            repository.getActivitiesForVehicle(name)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Baseline scan for current vehicle
    private val _currentVehicleBaseline = MutableStateFlow<EngineScan?>(null)
    val currentVehicleBaseline: StateFlow<EngineScan?> = _currentVehicleBaseline

    // Playback state
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio

    private val _audioPlaybackPositionMs = MutableStateFlow(0L)
    val audioPlaybackPositionMs: StateFlow<Long> = _audioPlaybackPositionMs

    private val _audioPlaybackDurationMs = MutableStateFlow(0L)
    val audioPlaybackDurationMs: StateFlow<Long> = _audioPlaybackDurationMs

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    init {
        // Automatically fetch baseline whenever vehicle name changes
        viewModelScope.launch {
            _vehicleName.collect { name ->
                refreshBaseline(name)
            }
        }
    }

    fun selectTab(tab: String) {
        _currentTab.value = tab
        if (tab == "Diagnose") {
            _selectedScan.value = null // Back to scanner interface
        }
    }

    fun setVehicle(name: String, type: String) {
        _vehicleName.value = name.ifBlank { "My Vehicle" }
        _vehicleType.value = type
        viewModelScope.launch {
            refreshBaseline(_vehicleName.value)
        }
    }

    fun selectScan(scan: EngineScan?) {
        stopAudio()
        _selectedScan.value = scan
    }

    private suspend fun refreshBaseline(name: String) {
        _currentVehicleBaseline.value = repository.getBaselineForVehicle(name)
    }

    fun startScanning(context: Context) {
        stopAudio()
        _aiInsight.value = null
        val isBaseline = _isCustomBaselineRequested.value && _currentVehicleBaseline.value == null
        
        processor.startScan(
            context = context,
            vehicleName = _vehicleName.value,
            vehicleType = _vehicleType.value,
            isBaseline = isBaseline,
            onComplete = { completedScan ->
                viewModelScope.launch {
                    repository.insertScan(completedScan)
                    
                    // Save to Room database DiagnosticRecord
                    val diagRecord = DiagnosticRecord(
                        vehicleName = completedScan.vehicleName,
                        timestamp = completedScan.timestamp,
                        soundFrequencyData = completedScan.rawAudioAnalysisSummary.ifBlank { "Peak Frequency Analysis Data" },
                        healthStatus = completedScan.urgency,
                        healthScore = completedScan.healthScore,
                        aiDiagnosticSummary = completedScan.issueName
                    )
                    repository.insertDiagnosticRecord(diagRecord)

                    refreshBaseline(_vehicleName.value)
                    _selectedScan.value = completedScan
                    _isCustomBaselineRequested.value = false // Reset baseline toggle
                    
                    // Sync to Firebase Firestore in background
                    FirebaseManager.syncScanToCloud(context, completedScan)
                }
            },
            onError = { errorMsg ->
                Log.e("EngineSoundVM", "Scan failed: $errorMsg")
            }
        )
    }

    fun fetchAiInsight(context: Context, scan: EngineScan) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiInsight.value = null
            when (val result = GeminiDiagnosticManager.analyzeEngineScan(context, scan)) {
                is GeminiResult.Success -> {
                    _aiInsight.value = result.data
                    val diagRecord = DiagnosticRecord(
                        vehicleName = scan.vehicleName,
                        timestamp = System.currentTimeMillis(),
                        soundFrequencyData = scan.rawAudioAnalysisSummary,
                        healthStatus = scan.urgency,
                        healthScore = scan.healthScore,
                        aiDiagnosticSummary = result.data
                    )
                    repository.insertDiagnosticRecord(diagRecord)
                }
                is GeminiResult.Error -> {
                    _aiInsight.value = "⚠️ ${result.userFriendlyMessage}"
                }
            }
            _isAiLoading.value = false
        }
    }

    fun analyzeRecordedAudioMemo(context: Context, audioFile: File) {
        _isAiLoading.value = true
        _aiInsight.value = null
        viewModelScope.launch {
            when (val result = GeminiDiagnosticManager.analyzeRecordedAudio(
                context = context,
                audioFile = audioFile,
                vehicleName = _vehicleName.value,
                vehicleType = _vehicleType.value
            )) {
                is GeminiResult.Success -> {
                    val scan = result.data
                    val rowId = repository.insertScan(scan)
                    val finalScan = scan.copy(id = rowId.toInt())
                    
                    val diagRecord = DiagnosticRecord(
                        vehicleName = finalScan.vehicleName,
                        timestamp = finalScan.timestamp,
                        soundFrequencyData = finalScan.rawAudioAnalysisSummary.ifBlank { "Peak Frequency Analysis Data" },
                        healthStatus = finalScan.urgency,
                        healthScore = finalScan.healthScore,
                        aiDiagnosticSummary = finalScan.issueName
                    )
                    repository.insertDiagnosticRecord(diagRecord)
                    refreshBaseline(_vehicleName.value)
                    _selectedScan.value = finalScan
                }
                is GeminiResult.Error -> {
                    // Fail gracefully
                }
            }
            _isAiLoading.value = false
        }
    }

    fun stopScanning() {
        processor.stopScan()
    }

    fun setBaselineRequest(request: Boolean) {
        _isCustomBaselineRequested.value = request
    }

    // Audio Playback logic for Recorded Sound Preview & Mechanic Cards
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    val playing = player.isPlaying
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                    if (!playing) player.pause()
                }
            } catch (e: Exception) {
                Log.e("EngineSoundVM", "Error setting speed: ${e.message}")
            }
        }
    }

    fun seekAudioTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _audioPlaybackPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e("EngineSoundVM", "Error seeking: ${e.message}")
        }
    }

    fun toggleAudioPlayback(filePath: String?) {
        if (filePath == null) return
        if (_isPlayingAudio.value) {
            stopAudio()
        } else {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w("EngineSoundVM", "Audio file does not exist: $filePath")
                return
            }
            try {
                playbackJob?.cancel()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        playbackParams = playbackParams.setSpeed(_playbackSpeed.value)
                    }
                    _audioPlaybackDurationMs.value = duration.toLong().coerceAtLeast(1000L)
                    _audioPlaybackPositionMs.value = 0L
                    start()
                    _isPlayingAudio.value = true
                    setOnCompletionListener {
                        stopAudio()
                    }
                }

                playbackJob = viewModelScope.launch {
                    while (_isPlayingAudio.value && mediaPlayer != null) {
                        try {
                            mediaPlayer?.let { player ->
                                if (player.isPlaying) {
                                    _audioPlaybackPositionMs.value = player.currentPosition.toLong()
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                        kotlinx.coroutines.delay(100)
                    }
                }
            } catch (e: Exception) {
                Log.e("EngineSoundVM", "Error playing sound file: ${e.message}")
                stopAudio()
            }
        }
    }

    fun stopAudio() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("EngineSoundVM", "Error releasing MediaPlayer: ${e.message}")
        } finally {
            mediaPlayer = null
            _isPlayingAudio.value = false
            _audioPlaybackPositionMs.value = 0L
        }
    }

    fun deleteScan(id: Int) {
        viewModelScope.launch {
            repository.deleteScanById(id)
            if (_selectedScan.value?.id == id) {
                _selectedScan.value = null
            }
            refreshBaseline(_vehicleName.value)
        }
    }

    fun addActivity(
        title: String,
        type: String, // "MAINTENANCE" or "REPAIR"
        status: String, // "COMPLETED" or "PLANNED"
        cost: Double,
        notes: String,
        mileage: Int,
        dateEpochMs: Long,
        intervalMiles: Int = 0,
        intervalDays: Int = 0
    ) {
        viewModelScope.launch {
            val activity = MaintenanceActivity(
                vehicleName = _vehicleName.value,
                title = title.ifBlank { "Routine Service" },
                type = type,
                status = status,
                dateEpochMs = dateEpochMs,
                cost = cost,
                notes = notes,
                mileage = mileage,
                intervalMiles = intervalMiles,
                intervalDays = intervalDays
            )
            repository.insertActivity(activity)
        }
    }

    fun updateActivity(activity: MaintenanceActivity) {
        viewModelScope.launch {
            repository.insertActivity(activity)
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            repository.deleteActivityById(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _selectedScan.value = null
            _currentVehicleBaseline.value = null
        }
    }

    // MediaRecorder API Integration Methods
    fun startMediaRecording(context: Context, maxSeconds: Int = 10) {
        stopAudio()
        if (mediaRecorderEngine == null) {
            mediaRecorderEngine = EngineMediaRecorder(context.applicationContext)
        }
        val recorder = mediaRecorderEngine!!
        recorder.startRecording(maxDurationSeconds = maxSeconds) { recordedFile ->
            _lastRecordedFile.value = recordedFile
            _isMediaRecording.value = false
        }

        viewModelScope.launch {
            launch { recorder.isRecording.collect { _isMediaRecording.value = it } }
            launch { recorder.recordingDurationMs.collect { _mediaRecordDurationMs.value = it } }
            launch { recorder.currentAmplitude.collect { _mediaRecordAmplitude.value = it } }
            launch { recorder.statusText.collect { _mediaRecordStatus.value = it } }
            launch { recorder.recordedFile.collect { _lastRecordedFile.value = it } }
        }
    }

    fun stopMediaRecording(): File? {
        val file = mediaRecorderEngine?.stopRecording()
        _isMediaRecording.value = false
        _lastRecordedFile.value = file
        return file
    }

    fun cancelMediaRecording() {
        mediaRecorderEngine?.cancelRecording()
        _isMediaRecording.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
        processor.stopScan()
        mediaRecorderEngine?.cancelRecording()
    }
}

class EngineSoundViewModelFactory(private val repository: EngineScanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EngineSoundViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EngineSoundViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
