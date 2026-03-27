package com.example.fraudlens.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val IPDB_BASE_URL = "https://api.abuseipdb.com/api/v2/"
    private const val MODEL_BASE_URL = "https://fraudlens-updated-model-api.onrender.com/"
    private const val RAZORPAY_BASE_URL = "https://api.razorpay.com/v1/"

    val loggingInterceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Model API client with extended timeout
    private val modelHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)  // 60 seconds
        .readTimeout(60, TimeUnit.SECONDS)     // 60 seconds
        .writeTimeout(60, TimeUnit.SECONDS)    // 60 seconds
        .build()

    private val IPDB_Builder by lazy {
        Retrofit.Builder()
            .baseUrl(IPDB_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val Model_API_Builder by lazy {
        Retrofit.Builder()
            .baseUrl(MODEL_BASE_URL)
            .client(modelHttpClient)  // Use the extended timeout client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val razorpay_API_Builder by lazy {
        Retrofit.Builder()
            .baseUrl(RAZORPAY_BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val IPDB_api: IPDBApi by lazy {
        IPDB_Builder.create(IPDBApi::class.java)
    }

    val Razorpay_api: RazorpayApi by lazy {
        razorpay_API_Builder.create(RazorpayApi::class.java)
    }

    val model_api: ModelAPI by lazy {
        Model_API_Builder.create(ModelAPI::class.java)
    }
}