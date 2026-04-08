package com.example.fraudlens.demo

import android.util.Base64
import com.example.fraudlens.BuildConfig
import com.example.fraudlens.retrofit.RazorpayApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Razorpay REST API requires Basic auth (key_id:key_secret).
 * [com.example.fraudlens.retrofit.RetrofitInstance] exposes Razorpay without auth — demo used that and got 401.
 * This client matches [com.example.fraudlens.module.RazorpayModule].
 */
object DemoRazorpayClient {

    private const val BASE_URL = "https://api.razorpay.com/v1/"

    val api: RazorpayApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val auth = Interceptor { chain ->
            val keyId = BuildConfig.RAZORPAY_KEY_ID
            val keySecret = BuildConfig.RAZORPAY_KEY_SECRET
            val credentials = "$keyId:$keySecret"
            val b64 = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val req = chain.request().newBuilder()
                .header("Authorization", "Basic $b64")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RazorpayApi::class.java)
    }
}
