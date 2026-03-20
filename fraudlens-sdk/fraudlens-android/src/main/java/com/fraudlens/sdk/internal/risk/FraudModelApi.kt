package com.fraudlens.sdk.internal.risk

import com.fraudlens.sdk.risk.ModelHealthOutput
import com.fraudlens.sdk.risk.ModelInput
import com.fraudlens.sdk.risk.ModelOutput
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

internal interface FraudModelApi {
    @Headers("Accept: application/json")
    @GET("health")
    suspend fun checkHealth(): Response<ModelHealthOutput>

    @Headers("Accept: application/json")
    @POST("predict")
    suspend fun predictFraud(@Body input: ModelInput): Response<ModelOutput>
}
