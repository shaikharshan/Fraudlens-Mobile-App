package com.fraudlens.sdk.internal.image

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

internal interface ImageDetectionApi {
    @GET("health")
    suspend fun health(): Response<ResponseBody>

    @Multipart
    @POST("api/image-detection")
    suspend fun detect(@Part file: MultipartBody.Part): Response<ResponseBody>
}
