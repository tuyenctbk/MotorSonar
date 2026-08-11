package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class EngineMediaRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var recordJob: Job? = null
    private var currentOutputFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs

    private val _currentAmplitude = MutableStateFlow(0)
    val currentAmplitude: StateFlow<Int> = _currentAmplitude

    private val _recordedFile = MutableStateFlow<File?>(null)
    val recordedFile: StateFlow<File?> = _recordedFile

    private val _statusText = MutableStateFlow("Ready to Record Engine Sound")
    val statusText: StateFlow<String> = _statusText

    fun startRecording(maxDurationSeconds: Int = 10, onMaxDurationReached: ((File) -> Unit)? = null): Boolean {
        if (_isRecording.value) return false

        try {
            val file = File(context.cacheDir, "engine_media_record_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setOutputFile(file.absolutePath)

            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            _statusText.value = "Recording Engine Audio via MediaRecorder..."

            recordJob = CoroutineScope(Dispatchers.IO).launch {
                val startTime = System.currentTimeMillis()
                val maxMs = maxDurationSeconds * 1000L

                while (_isRecording.value && isActive) {
                    val elapsed = System.currentTimeMillis() - startTime
                    _recordingDurationMs.value = elapsed

                    try {
                        val amp = mediaRecorder?.maxAmplitude ?: 0
                        _currentAmplitude.value = amp
                    } catch (e: Exception) {
                        Log.e("EngineMediaRecorder", "Error fetching amplitude", e)
                    }

                    if (elapsed >= maxMs) {
                        withContext(Dispatchers.Main) {
                            val recorded = stopRecording()
                            if (recorded != null) {
                                onMaxDurationReached?.invoke(recorded)
                            }
                        }
                        break
                    }

                    delay(100L)
                }
            }

            return true
        } catch (e: Exception) {
            Log.e("EngineMediaRecorder", "Failed to start MediaRecorder recording", e)
            _statusText.value = "Recording Failed: ${e.localizedMessage ?: "Hardware/Permission issue"}"
            cleanup()
            return false
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return currentOutputFile

        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            _statusText.value = "Engine Audio Recorded Successfully"
            _recordedFile.value = currentOutputFile
            Log.d("EngineMediaRecorder", "Recording stopped. File saved at: ${currentOutputFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("EngineMediaRecorder", "Error stopping MediaRecorder", e)
            _statusText.value = "Recording Stopped with Warning"
        } finally {
            mediaRecorder = null
        }

        return currentOutputFile
    }

    fun cancelRecording() {
        if (_isRecording.value) {
            _isRecording.value = false
            recordJob?.cancel()
            recordJob = null

            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            } catch (e: Exception) {
                Log.e("EngineMediaRecorder", "Error releasing MediaRecorder on cancel", e)
            } finally {
                mediaRecorder = null
            }
        }

        currentOutputFile?.delete()
        currentOutputFile = null
        _recordedFile.value = null
        _recordingDurationMs.value = 0L
        _currentAmplitude.value = 0
        _statusText.value = "Recording Cancelled"
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e("EngineMediaRecorder", "Cleanup error", e)
        }
        mediaRecorder = null
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
    }
}
