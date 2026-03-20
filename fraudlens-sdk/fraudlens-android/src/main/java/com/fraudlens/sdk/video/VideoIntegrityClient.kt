package com.fraudlens.sdk.video

import com.fraudlens.sdk.image.model.MediaUploadRequest
import com.fraudlens.sdk.internal.video.VideoDetectionApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

interface VideoIntegrityClient {
    suspend fun health(): Result<String>
    suspend fun detectVideo(request: MediaUploadRequest): Result<String>
}

internal class VideoIntegrityClientImpl(
    private val api: VideoDetectionApi,
) : VideoIntegrityClient {

    override suspend fun health(): Result<String> = runCatching {
        val res = api.health()
        if (!res.isSuccessful) error("video health failed: ${res.code()}")
        res.body()?.string().orEmpty()
    }

    override suspend fun detectVideo(request: MediaUploadRequest): Result<String> = runCatching {
        val mediaType = request.contentType.toMediaTypeOrNull()
            ?: "application/octet-stream".toMediaTypeOrNull()!!
        val body = request.bytes.toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData(request.partName, request.filename, body)
        val res = api.detect(part)
        if (!res.isSuccessful) error("video detection failed: ${res.code()}")
        res.body()?.string().orEmpty()
    }
}
