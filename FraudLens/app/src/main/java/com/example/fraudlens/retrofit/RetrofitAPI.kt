package com.example.fraudlens.retrofit



import com.example.fraudlens.BuildConfig
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface IPDBApi{

    @Headers(
    "Accept:application/json"
    )
    @GET("check")
    suspend fun checkIP(
        @Header("Key") key: String = BuildConfig.IPDB_API_KEY,
        @Query("ipAddress") ip: String,
        @Query("maxAgeInDays") days: Int = 90
    ): Response<AbuseIPCheckResponse>


    @Headers(
        "Accept:application/json"
    )
    @GET("reports")
    suspend fun getReports(
        @Header("Key") key: String = BuildConfig.IPDB_API_KEY,
        @Query("ipAddress") ipAddress: String
    ): Response<AbuseIPReportResponse>

    @Headers(
        "Accept: text/plain"
    )
    @GET("blacklist")
    suspend fun getBlacklistPlain(
        @Header("Key") key: String = BuildConfig.IPDB_API_KEY
    ): Response<ResponseBody>

}


interface ModelAPI {

    @Headers("Accept:application/json")
    @GET("health")
    suspend fun checkHealth(): Response<ModelHealthOutput>

    @Headers("Accept:application/json")
    @POST("predict")
    suspend fun predictFraud(@Body input: ModelInput)  : Response<ModelOutput>
}

interface GeminiAPI{
    @Headers(
        "Content-Type:application/json"
        )
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun invoke(
        @Header("x-goog-api-key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}


//interface RazorpayApi {
//
//    @Headers("Content-Type: application/json")
//    @POST("orders")
//    suspend fun createOrder(
//        @Header("Authorization") authorization: String,
//        @Body orderRequest: RazorpayOrderRequest
//    ): Response<RazorpayOrderResponse>
//}

interface RazorpayApi {

    @POST("orders")
    suspend fun createOrder(
        @Body orderRequest: RazorpayOrderRequest
    ): Response<RazorpayOrderResponse>
}