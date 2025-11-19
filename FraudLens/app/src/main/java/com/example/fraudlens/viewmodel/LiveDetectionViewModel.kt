package com.example.fraudlens.viewmodel

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fraudlens.ui.components.AnalysisResult
import com.example.fraudlens.ui.components.AudioRecorder
import com.example.fraudlens.ui.components.GeminiApiManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class AlertType {
    AI_DETECTION,
    PATTERN_MATCH
}

enum class BatchMode(
    val silenceMs: Long,
    val minAudioLengthSeconds: Double,
    val apiCooldownMs: Long,
    val description: String
) {
    AGGRESSIVE(3000, 3.0, 8000, "Max savings - 3s silence, 8s cooldown"),
    BALANCED(2000, 2.0, 5000, "Balanced - 2s silence, 5s cooldown"),
    SENSITIVE(1500, 1.0, 3000, "More responsive - 1.5s silence, 3s cooldown")
}

data class FraudAlert(
    val id: Long,
    val timestamp: String,
    val confidence: Float,
    val reasoning: String,
    val recommendation: String,
    val detectedText: String,
    val type: AlertType
)

data class LiveDetectionUiState(
    val isRecording: Boolean = false,
    val isConnected: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val audioLevel: Float = 0f,
    val transcript: String = "",
    val fraudAlerts: List<FraudAlert> = emptyList(),
    val errorMessage: String? = null,
    val lastActivity: String? = null,
    val hasAudioPermission: Boolean = false,
    val apiCallCount: Int = 0,
    val batchMode: BatchMode = BatchMode.BALANCED
)

@HiltViewModel
class LiveDetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val geminiApiManager: GeminiApiManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveDetectionUiState())
    val uiState: StateFlow<LiveDetectionUiState> = _uiState.asStateFlow()

    private var alarmPlayer: MediaPlayer? = null
    private var apiKey: String = ""

    companion object {
        private const val TAG = "LiveDetectionVM"
    }

    init {
        checkAudioPermission()

        // Observe audio level
        viewModelScope.launch {
            audioRecorder.audioLevel.collect { level ->
                _uiState.value = _uiState.value.copy(audioLevel = level)
            }
        }
    }

    fun checkAudioPermission() {
        val hasPermission = audioRecorder.hasPermission()
        _uiState.value = _uiState.value.copy(hasAudioPermission = hasPermission)

        if (!hasPermission) {
            Log.w(TAG, "Audio permission not granted")
        }
    }

    fun saveApiKey(key: String) {
        apiKey = key
        Log.d(TAG, "API key saved")
    }

    fun setBatchMode(mode: BatchMode) {
        _uiState.value = _uiState.value.copy(batchMode = mode)
        Log.d(TAG, "Batch mode set to: $mode")
    }

    fun startMonitoring(key: String) {
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter your Gemini API key"
            )
            return
        }

        if (!audioRecorder.hasPermission()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Audio recording permission not granted"
            )
            return
        }

        apiKey = key
        geminiApiManager.resetApiCallCount()

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.CONNECTING,
                    errorMessage = null,
                    transcript = "",
                    fraudAlerts = emptyList(),
                    apiCallCount = 0
                )

                Log.d(TAG, "🚀 Starting monitoring with ${_uiState.value.batchMode} mode")

                val batchMode = _uiState.value.batchMode

                audioRecorder.startRecording(
                    silenceMs = batchMode.silenceMs,
                    onAudioChunk = { audioData ->
                        handleAudioChunk(audioData)
                    }
                )

                _uiState.value = _uiState.value.copy(
                    isRecording = true,
                    isConnected = true,
                    connectionStatus = ConnectionStatus.CONNECTED
                )

                Log.d(TAG, "✅ Monitoring started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting monitoring: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = "Failed to start monitoring: ${e.message}",
                    isRecording = false,
                    isConnected = false
                )
            }
        }
    }

    private fun handleAudioChunk(audioData: ByteArray) {
        viewModelScope.launch {
            val batchMode = _uiState.value.batchMode

            val result = geminiApiManager.analyzeAudioBatch(
                audioData = audioData,
                apiKey = apiKey,
                apiCooldownMs = batchMode.apiCooldownMs
            )

            _uiState.value = _uiState.value.copy(
                apiCallCount = geminiApiManager.getApiCallCount()
            )

            when (result) {
                is AnalysisResult.Success -> {
                    val timestamp = getCurrentTimestamp()

                    // Add to transcript
                    val transcriptEntry = "[$timestamp] ${result.rawResponse}\n"
                    _uiState.value = _uiState.value.copy(
                        transcript = _uiState.value.transcript + transcriptEntry,
                        lastActivity = timestamp
                    )

                    // Create fraud alert
                    val alert = FraudAlert(
                        id = System.currentTimeMillis(),
                        timestamp = timestamp,
                        confidence = result.analysis.confidence_score,
                        reasoning = result.analysis.reasoning,
                        recommendation = result.analysis.recommendation,
                        detectedText = result.rawResponse.take(200),
                        type = AlertType.AI_DETECTION
                    )

                    val currentAlerts = _uiState.value.fraudAlerts
                    _uiState.value = _uiState.value.copy(
                        fraudAlerts = listOf(alert) + currentAlerts.take(4)
                    )

                    playAlarmSound()
                    Log.d(TAG, "🚨 Fraud alert triggered!")
                }

                is AnalysisResult.Error -> {
                    Log.e(TAG, "Analysis error: ${result.message}")
                    val timestamp = getCurrentTimestamp()
                    val transcriptEntry = "[$timestamp] Error: ${result.message}\n"
                    _uiState.value = _uiState.value.copy(
                        transcript = _uiState.value.transcript + transcriptEntry
                    )
                }

                is AnalysisResult.RateLimitExceeded -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "⚠️ Rate limit exceeded! Switch to 'Aggressive' mode or wait."
                    )
                }

                is AnalysisResult.NoScamDetected -> {
                    Log.d(TAG, "✅ No scam detected in this batch")
                }
            }
        }
    }

    fun stopMonitoring() {
        Log.d(TAG, "🛑 Stopping monitoring...")

        audioRecorder.stopRecording()

        _uiState.value = _uiState.value.copy(
            isRecording = false,
            isConnected = false,
            connectionStatus = ConnectionStatus.DISCONNECTED,
            audioLevel = 0f
        )

        Log.d(TAG, "✅ Monitoring stopped")
    }

    fun clearTranscript() {
        _uiState.value = _uiState.value.copy(transcript = "")
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun triggerTestCapture() {
        if (!_uiState.value.isRecording) {
            Log.w(TAG, "Not recording, cannot test capture")
            return
        }
        Log.d(TAG, "🧪 Triggering test audio capture...")
        audioRecorder.triggerTestCapture()
    }

    fun enableAlwaysCaptureMode() {
        if (!_uiState.value.isRecording) {
            Log.w(TAG, "Not recording, cannot enable always capture")
            return
        }
        Log.d(TAG, "🔴 Enabling always capture mode...")
        audioRecorder.enableAlwaysCaptureMode()
    }

    fun disableAlwaysCaptureMode() {
        Log.d(TAG, "⚪ Disabling always capture mode...")
        audioRecorder.disableAlwaysCaptureMode()
    }

    private fun playAlarmSound() {
        try {
            // You can use a default system sound or load a custom alarm
            // For now, using a simple notification sound
            alarmPlayer?.release()
            alarmPlayer = MediaPlayer.create(
                context,
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
            )
            alarmPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm: ${e.message}")
        }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
        alarmPlayer?.release()
        alarmPlayer = null
    }
}