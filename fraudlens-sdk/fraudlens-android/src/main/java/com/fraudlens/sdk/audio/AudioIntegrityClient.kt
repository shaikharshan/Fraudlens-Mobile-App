package com.fraudlens.sdk.audio

import com.fraudlens.sdk.audio.model.VoiceDetectionRequest
import com.fraudlens.sdk.internal.audio.VoiceDetectionApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Voice / deepfake-related calls against the configured audio base URL.
 */
interface AudioIntegrityClient {

    /** Health check; returns raw JSON string from `/health`. */
    suspend fun health(): Result<String>

    /**
     * Uploads audio for analysis. Returns raw JSON string from `/api/voice-detection`.
     */
    suspend fun detectVoice(request: VoiceDetectionRequest): Result<String>
}

internal class AudioIntegrityClientImpl(
    private val api: VoiceDetectionApi,
) : AudioIntegrityClient {

    override suspend fun health(): Result<String> =
        runCatching {
            val res = api.health()
            if (!res.isSuccessful) {
                error("health failed: ${res.code()} ${res.message()}")
            }
            res.body()?.string().orEmpty()
        }

    override suspend fun detectVoice(request: VoiceDetectionRequest): Result<String> =
        runCatching {
            val mediaType = request.contentType.toMediaTypeOrNull()
                ?: "application/octet-stream".toMediaTypeOrNull()!!
            val body = request.audioBytes.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData(
                request.partName,
                request.filename,
                body,
            )
            val res = api.detectVoice(part)
            if (!res.isSuccessful) {
                error("voice-detection failed: ${res.code()} ${res.message()}")
            }
            res.body()?.string().orEmpty()
        }
}
