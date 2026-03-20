package com.fraudlens.sdk.internal.audio

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit binding for the voice service. Align [file] part name with your Space README if different.
 */
internal interface VoiceDetectionApi {

    @GET("health")
    suspend fun health(): Response<ResponseBody>

    @Multipart
    @POST("api/voice-detection")
    suspend fun detectVoice(
        @Part file: MultipartBody.Part,
    ): Response<ResponseBody>
}
