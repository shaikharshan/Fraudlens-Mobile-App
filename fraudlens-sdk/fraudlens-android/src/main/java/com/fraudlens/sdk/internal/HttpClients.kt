package com.fraudlens.sdk.internal

import com.fraudlens.sdk.FraudLensConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal fun FraudLensConfig.loggingInterceptor(): HttpLoggingInterceptor =
    HttpLoggingInterceptor().apply {
        level = if (enableHttpLogging) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

internal fun baseOkHttpClient(
    config: FraudLensConfig,
    vararg interceptors: Interceptor,
): OkHttpClient {
    val b = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
    interceptors.forEach { b.addInterceptor(it) }
    if (config.enableHttpLogging) {
        b.addInterceptor(config.loggingInterceptor())
    }
    return b.build()
}

internal fun headerInterceptor(name: String, valueProvider: () -> String?): Interceptor =
    Interceptor { chain ->
        val key = valueProvider()
        val req = if (key.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header(name, key).build()
        }
        chain.proceed(req)
    }

internal fun gsonRetrofit(client: OkHttpClient, baseUrl: String): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl.withTrailingSlash())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

internal fun plainRetrofit(client: OkHttpClient, baseUrl: String): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl.withTrailingSlash())
        .client(client)
        .build()
