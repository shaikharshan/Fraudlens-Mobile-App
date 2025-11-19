package com.example.fraudlens.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class AudioRecorder @Inject constructor(
    private val context: Context
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var silenceDetectionJob: Job? = null

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    private var onAudioChunkCallback: ((ByteArray) -> Unit)? = null

    // Voice Activity Detection
    private var isSpeaking = false
    private val audioBuffer = ByteArrayOutputStream()
    private var lastSpeechTime = 0L

    // Test mode - force capture even with low audio
    private var testMode = false
    private var testModeStartTime = 0L

    // Always capture mode for debugging
    private var alwaysCaptureMode = false

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2

        // Voice Activity Detection thresholds (VERY LOW for maximum sensitivity)
        private const val SPEECH_THRESHOLD = 0.001f // Very sensitive
        private const val MIN_AUDIO_LENGTH_SECONDS = 1.0 // Minimum 1 second
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        silenceMs: Long = 2000,
        onAudioChunk: (ByteArray) -> Unit
    ) {
        if (!hasPermission()) {
            Log.e(TAG, "❌ Audio recording permission not granted")
            return
        }

        if (audioRecord != null) {
            Log.w(TAG, "⚠️ Recording already in progress")
            return
        }

        onAudioChunkCallback = onAudioChunk

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )

            if (minBufferSize <= 0) {
                Log.e(TAG, "❌ Invalid buffer size: $minBufferSize")
                return
            }

            val bufferSize = minBufferSize * BUFFER_SIZE_FACTOR
            Log.d(TAG, "📊 Buffer size: $bufferSize bytes (min: $minBufferSize)")

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "❌ AudioRecord initialization failed - state: ${audioRecord?.state}")
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            Log.d(TAG, "✅ AudioRecord started successfully")
            Log.d(TAG, "🎤 Recording started with ${silenceMs}ms silence threshold")
            Log.d(TAG, "🎯 Speech threshold: $SPEECH_THRESHOLD (very sensitive)")
            Log.d(TAG, "⏱️ Minimum duration: ${MIN_AUDIO_LENGTH_SECONDS}s")

            // Audio capture loop
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(4096)
                var frameCount = 0
                var totalBytesRead = 0L

                while (isActive) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                    if (readBytes > 0) {
                        totalBytesRead += readBytes
                        val audioChunk = buffer.copyOf(readBytes)

                        // Calculate audio level
                        val level = calculateAudioLevel(audioChunk, readBytes)
                        _audioLevel.value = level

                        // Log every 50 frames (~0.5 seconds)
                        frameCount++
                        if (frameCount % 50 == 0) {
                            Log.d(TAG, "🔊 Audio level: ${String.format("%.6f", level)} | Threshold: ${String.format("%.6f", SPEECH_THRESHOLD)} | Total: ${totalBytesRead} bytes")
                        }

                        // Voice Activity Detection
                        processAudioForVAD(audioChunk, level, silenceMs)
                    } else if (readBytes < 0) {
                        Log.e(TAG, "❌ Error reading audio: $readBytes")
                    }

                    delay(10)
                }
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception - no permission: ${e.message}", e)
            stopRecording()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting recording: ${e.message}", e)
            stopRecording()
        }
    }

    private suspend fun processAudioForVAD(
        audioChunk: ByteArray,
        level: Float,
        silenceMs: Long
    ) = withContext(Dispatchers.Default) {
        val currentTime = System.currentTimeMillis()

        // Test mode: Always capture for 5 seconds
        if (testMode) {
            audioBuffer.write(audioChunk)
            if (currentTime - testModeStartTime > 5000) {
                Log.d(TAG, "🧪 TEST MODE: 5 seconds elapsed, processing...")
                processAudioBatch()
                testMode = false
            }
            return@withContext
        }

        // Always capture mode for debugging
        if (alwaysCaptureMode) {
            if (!isSpeaking) {
                Log.d(TAG, "🎤 ALWAYS CAPTURE MODE: Starting buffer")
                isSpeaking = true
                audioBuffer.reset()
            }
            audioBuffer.write(audioChunk)

            // Process every 3 seconds in always capture mode
            if (audioBuffer.size() >= SAMPLE_RATE * 2 * 3) {
                Log.d(TAG, "🎤 ALWAYS CAPTURE MODE: Processing 3-second chunk")
                processAudioBatch()
            }
            return@withContext
        }

        // Normal VAD mode
        if (level > SPEECH_THRESHOLD) {
            // Speech detected
            if (!isSpeaking) {
                Log.d(TAG, "🎤 ===== SPEECH DETECTED =====")
                Log.d(TAG, "   Level: ${String.format("%.6f", level)} | Threshold: ${String.format("%.6f", SPEECH_THRESHOLD)}")
                isSpeaking = true
                audioBuffer.reset()
            }

            // Add audio to buffer
            audioBuffer.write(audioChunk)
            lastSpeechTime = currentTime

            // Log buffer size every 64KB
            if (audioBuffer.size() % 64000 < 4096) {
                val durationSoFar = audioBuffer.size() / (SAMPLE_RATE * 2.0)
                Log.d(TAG, "   📦 Buffering... ${audioBuffer.size()} bytes (~${String.format("%.1f", durationSoFar)}s)")
            }

            // Cancel any existing silence timeout
            silenceDetectionJob?.cancel()

            // Start new silence detection
            silenceDetectionJob = CoroutineScope(Dispatchers.IO).launch {
                delay(silenceMs)

                // If we reach here, silence period has passed
                if (isSpeaking) {
                    Log.d(TAG, "⏸️ Silence detected (${silenceMs}ms) - processing audio batch")
                    processAudioBatch()
                }
            }
        } else {
            // Log low audio levels to help debug
            if (level > SPEECH_THRESHOLD * 0.1f && currentTime % 2000 < 100) {
                Log.d(TAG, "🔉 Below threshold: ${String.format("%.6f", level)} (need ${String.format("%.6f", SPEECH_THRESHOLD)})")
            }
        }
    }

    private fun processAudioBatch() {
        val audioData = audioBuffer.toByteArray()
        audioBuffer.reset()
        isSpeaking = false

        // Calculate duration
        val durationSeconds = audioData.size / (SAMPLE_RATE * 2.0)

        Log.d(TAG, "📊 Audio batch ready:")
        Log.d(TAG, "   Size: ${audioData.size} bytes")
        Log.d(TAG, "   Duration: ${String.format("%.2f", durationSeconds)}s")
        Log.d(TAG, "   Sample rate: $SAMPLE_RATE Hz")

        if (durationSeconds >= MIN_AUDIO_LENGTH_SECONDS) {
            Log.d(TAG, "✅ Audio length OK (${String.format("%.2f", durationSeconds)}s >= ${MIN_AUDIO_LENGTH_SECONDS}s)")
            Log.d(TAG, "📤 Sending ${audioData.size} bytes for analysis...")
            onAudioChunkCallback?.invoke(audioData)
        } else {
            Log.d(TAG, "⏩ Audio too short (${String.format("%.2f", durationSeconds)}s < ${MIN_AUDIO_LENGTH_SECONDS}s) - SKIPPING")
        }
    }

    fun stopRecording() {
        Log.d(TAG, "🛑 Stopping recording...")

        recordingJob?.cancel()
        recordingJob = null

        silenceDetectionJob?.cancel()
        silenceDetectionJob = null

        audioRecord?.apply {
            try {
                if (state == AudioRecord.STATE_INITIALIZED) {
                    stop()
                }
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording: ${e.message}", e)
            }
        }
        audioRecord = null

        audioBuffer.reset()
        isSpeaking = false
        testMode = false
        alwaysCaptureMode = false
        _audioLevel.value = 0f
        onAudioChunkCallback = null

        Log.d(TAG, "✅ Recording stopped and cleaned up")
    }

    private fun calculateAudioLevel(buffer: ByteArray, readBytes: Int): Float {
        return try {
            var sum = 0.0
            var count = 0

            for (i in 0 until readBytes - 1 step 2) {
                val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
                sum += sample * sample
                count++
            }

            if (count == 0) return 0f

            val rms = sqrt(sum / count)
            val normalized = (rms / 32768.0).toFloat()

            normalized.coerceIn(0f, 1f)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating audio level: ${e.message}")
            0f
        }
    }

    fun isRecording(): Boolean = audioRecord != null && recordingJob?.isActive == true

    // Test mode: Force audio capture for 5 seconds
    fun triggerTestCapture() {
        if (!isRecording()) {
            Log.w(TAG, "⚠️ Not recording, cannot trigger test capture")
            return
        }

        Log.d(TAG, "🧪 ===== TEST MODE ACTIVATED =====")
        Log.d(TAG, "🧪 Forcing 5-second audio capture (ignoring speech detection)...")
        testMode = true
        testModeStartTime = System.currentTimeMillis()
        audioBuffer.reset()
        isSpeaking = true
    }

    // Always capture mode: Continuously capture audio every 3 seconds
    fun enableAlwaysCaptureMode() {
        if (!isRecording()) {
            Log.w(TAG, "⚠️ Not recording, cannot enable always capture mode")
            return
        }

        Log.d(TAG, "🔴 ===== ALWAYS CAPTURE MODE ENABLED =====")
        Log.d(TAG, "🔴 Will process audio every 3 seconds regardless of volume")
        alwaysCaptureMode = true
        audioBuffer.reset()
    }

    fun disableAlwaysCaptureMode() {
        Log.d(TAG, "⚪ Always capture mode disabled")
        alwaysCaptureMode = false
        audioBuffer.reset()
        isSpeaking = false
    }
}