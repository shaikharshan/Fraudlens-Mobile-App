package com.example.fraudlens.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val IPDB_BASE_URL = "https://api.abuseipdb.com/api/v2/"
    private const val MODEL_BASE_URL = "https://fraudlens-fastapi.onrender.com/"

    val loggingInterceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
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
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    val IPDB_api: IPDBApi by lazy {
        IPDB_Builder.create(IPDBApi::class.java)
    }

    val model_api: ModelAPI by lazy{
        Model_API_Builder.create(ModelAPI::class.java)
    }


}