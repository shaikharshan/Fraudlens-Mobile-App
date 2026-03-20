package com.fraudlens.sdk.internal.scam

import com.fraudlens.sdk.scam.GeminiRequest
import com.fraudlens.sdk.scam.GeminiResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface GeminiRestApi {
    @Headers("Content-Type: application/json")
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(@Body request: GeminiRequest): Response<GeminiResponse>
}
