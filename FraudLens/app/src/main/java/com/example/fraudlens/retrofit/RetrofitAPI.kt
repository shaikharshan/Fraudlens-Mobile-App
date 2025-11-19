package com.example.fraudlens.retrofit



import com.example.fraudlens.API_KEYS
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

const val IPDB_API_KEY:String = API_KEYS.IPDB_API_KEY
const val GEMINI_API_KEY:String = API_KEYS.GEMINI_API_KEY

interface IPDBApi{

    @Headers(
    "Accept:application/json",
    "Key:${IPDB_API_KEY}"
    )
    @GET("check")
    suspend fun checkIP(
        @Query("ipAddress") ip: String,
        @Query("maxAgeInDays") days: Int = 90
    ): Response<AbuseIPCheckResponse>


    @Headers(
        "Accept:application/json",
        "Key:${IPDB_API_KEY}"
    )
    @GET("reports")
    suspend fun getReports(
        @Query("ipAddress") ipAddress: String
    ): Response<AbuseIPReportResponse>

    @Headers(
        "Accept: text/plain",
        "Key:${IPDB_API_KEY}"
    )
    @GET("blacklist")
    suspend fun getBlacklistPlain(): Response<ResponseBody>

}


interface ModelAPI {

    @Headers("Accept:application/json")
    @GET("health")
    suspend fun checkHealth(): Response<ModelHealthOutput>

    @Headers("Accept:application/json")
    @POST("/predict")
    suspend fun predictFraud(@Body input: ModelInput)  : Response<ModelOutput>
}

interface GeminiAPI{
    @Headers(
        "Content-Type:application/json",
        "x-goog-api-key:${GEMINI_API_KEY}"
        )
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun invoke(
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}

