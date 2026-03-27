package com.example.fraudlens.module

import android.util.Base64
import com.example.fraudlens.BuildConfig
import com.example.fraudlens.retrofit.RazorpayApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RazorpayModule {

    private const val RAZORPAY_BASE_URL = "https://api.razorpay.com/v1/"

    private val RAZORPAY_KEY_ID = BuildConfig.RAZORPAY_KEY_ID
    private val RAZORPAY_KEY_SECRET = BuildConfig.RAZORPAY_KEY_SECRET

    @Provides
    @Singleton
    @RazorpayClient
    fun provideRazorpayOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        }

        val authInterceptor = Interceptor { chain ->
            val credentials = "$RAZORPAY_KEY_ID:$RAZORPAY_KEY_SECRET"
            val base64Credentials = Base64.encodeToString(
                credentials.toByteArray(),
                Base64.NO_WRAP
            )

            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .header("Authorization", "Basic $base64Credentials")
                .header("Content-Type", "application/json")
                .build()

            chain.proceed(newRequest)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @RazorpayRetrofit
    fun provideRazorpayRetrofit(
        @RazorpayClient okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(RAZORPAY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRazorpayApi(@RazorpayRetrofit retrofit: Retrofit): RazorpayApi {
        return retrofit.create(RazorpayApi::class.java)
    }
}