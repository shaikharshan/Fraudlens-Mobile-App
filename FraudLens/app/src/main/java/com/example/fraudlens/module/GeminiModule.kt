package com.example.fraudlens.module

import android.content.Context
import com.example.fraudlens.retrofit.GeminiAPI
import com.example.fraudlens.ui.components.AudioRecorder
import com.example.fraudlens.ui.components.GeminiApiManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    @Provides
    @Singleton
    @GeminiClient  // Add qualifier
    fun provideGeminiOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    @GeminiRetrofit  // Add qualifier
    fun provideGeminiRetrofit(
        @GeminiClient okHttpClient: OkHttpClient,  // Use qualifier
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideGeminiAPI(@GeminiRetrofit retrofit: Retrofit): GeminiAPI {  // Use qualifier
        return retrofit.create(GeminiAPI::class.java)
    }

    @Provides
    @Singleton
    fun provideAudioRecorder(
        @ApplicationContext context: Context
    ): AudioRecorder {
        return AudioRecorder(context)
    }

    @Provides
    @Singleton
    fun provideGeminiApiManager(
        @GeminiClient okHttpClient: OkHttpClient,  // Use qualifier
        gson: Gson
    ): GeminiApiManager {
        return GeminiApiManager(okHttpClient, gson)
    }

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}