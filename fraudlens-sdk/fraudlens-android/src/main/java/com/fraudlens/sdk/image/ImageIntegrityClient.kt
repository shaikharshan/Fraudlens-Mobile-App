package com.fraudlens.sdk.image

import com.fraudlens.sdk.image.model.MediaUploadRequest
import com.fraudlens.sdk.internal.image.ImageDetectionApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

interface ImageIntegrityClient {
    suspend fun health(): Result<String>
    suspend fun detectImage(request: MediaUploadRequest): Result<String>
}

internal class ImageIntegrityClientImpl(
    private val api: ImageDetectionApi,
) : ImageIntegrityClient {

    override suspend fun health(): Result<String> = runCatching {
        val res = api.health()
        if (!res.isSuccessful) error("image health failed: ${res.code()}")
        res.body()?.string().orEmpty()
    }

    override suspend fun detectImage(request: MediaUploadRequest): Result<String> = runCatching {
        val mediaType = request.contentType.toMediaTypeOrNull()
            ?: "application/octet-stream".toMediaTypeOrNull()!!
        val body = request.bytes.toRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData(request.partName, request.filename, body)
        val res = api.detect(part)
        if (!res.isSuccessful) error("image detection failed: ${res.code()}")
        res.body()?.string().orEmpty()
    }
}
