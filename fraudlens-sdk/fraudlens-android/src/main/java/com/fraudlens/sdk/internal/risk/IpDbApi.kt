package com.fraudlens.sdk.internal.risk

import com.fraudlens.sdk.risk.AbuseIPCheckResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

internal interface IpDbApi {
    @Headers("Accept: application/json")
    @GET("check")
    suspend fun checkIP(
        @Query("ipAddress") ip: String,
        @Query("maxAgeInDays") days: Int = 90,
    ): Response<AbuseIPCheckResponse>
}
