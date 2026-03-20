package com.fraudlens.sdk

import com.fraudlens.sdk.audio.AudioIntegrityClient
import com.fraudlens.sdk.audio.AudioIntegrityClientImpl
import com.fraudlens.sdk.firebase.FraudLensFirestoreRepository
import com.fraudlens.sdk.image.ImageIntegrityClient
import com.fraudlens.sdk.image.ImageIntegrityClientImpl
import com.fraudlens.sdk.internal.audio.VoiceDetectionApi
import com.fraudlens.sdk.internal.baseOkHttpClient
import com.fraudlens.sdk.internal.gsonRetrofit
import com.fraudlens.sdk.internal.headerInterceptor
import com.fraudlens.sdk.internal.image.ImageDetectionApi
import com.fraudlens.sdk.internal.plainRetrofit
import com.fraudlens.sdk.internal.risk.FraudModelApi
import com.fraudlens.sdk.internal.risk.IpDbApi
import com.fraudlens.sdk.internal.scam.GeminiRestApi
import com.fraudlens.sdk.internal.video.VideoDetectionApi
import com.fraudlens.sdk.risk.PaymentRiskClient
import com.fraudlens.sdk.risk.PaymentRiskClientImpl
import com.fraudlens.sdk.scam.ScamAnalysisClient
import com.fraudlens.sdk.scam.ScamAnalysisClientImpl
import com.fraudlens.sdk.video.VideoIntegrityClient
import com.fraudlens.sdk.video.VideoIntegrityClientImpl
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.Interceptor

/**
 * Entry point for the FraudLens Android SDK.
 * Call [initialize] before using accessors (e.g. in [android.app.Application.onCreate] **before** `super.onCreate()` if you use Hilt-injected clients).
 */
object FraudLensSdk {

    @Volatile
    private var config: FraudLensConfig? = null

    @Volatile
    private var audioClient: AudioIntegrityClient? = null

    @Volatile
    private var imageClient: ImageIntegrityClient? = null

    @Volatile
    private var videoClient: VideoIntegrityClient? = null

    @Volatile
    private var paymentRiskClient: PaymentRiskClient? = null

    @Volatile
    private var scamClient: ScamAnalysisClient? = null

    @Volatile
    private var firestoreRepository: FraudLensFirestoreRepository? = null

    @Synchronized
    fun initialize(configuration: FraudLensConfig) {
        config = configuration

        if (configuration.audioBaseUrl.isNotBlank()) {
            val keyInterceptor = headerInterceptor("x-api-key") { configuration.audioApiKey }
            val client = baseOkHttpClient(configuration, keyInterceptor)
            val retrofit = plainRetrofit(client, configuration.audioBaseUrl)
            audioClient = AudioIntegrityClientImpl(retrofit.create(VoiceDetectionApi::class.java))
        } else {
            audioClient = null
        }

        if (configuration.imageBaseUrl.isNotBlank()) {
            val keyInterceptor = headerInterceptor("x-api-key") { configuration.imageApiKey }
            val client = baseOkHttpClient(configuration, keyInterceptor)
            val retrofit = plainRetrofit(client, configuration.imageBaseUrl)
            imageClient = ImageIntegrityClientImpl(retrofit.create(ImageDetectionApi::class.java))
        } else {
            imageClient = null
        }

        if (configuration.videoBaseUrl.isNotBlank()) {
            val keyInterceptor = headerInterceptor("x-api-key") { configuration.videoApiKey }
            val client = baseOkHttpClient(configuration, keyInterceptor)
            val retrofit = plainRetrofit(client, configuration.videoBaseUrl)
            videoClient = VideoIntegrityClientImpl(retrofit.create(VideoDetectionApi::class.java))
        } else {
            videoClient = null
        }

        val ipApi: IpDbApi? = configuration.abuseIpDbApiKey?.takeIf { it.isNotBlank() }?.let { key ->
            val ipInterceptor = Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Key", key)
                        .header("Accept", "application/json")
                        .build(),
                )
            }
            val client = baseOkHttpClient(configuration, ipInterceptor)
            gsonRetrofit(client, configuration.abuseIpDbBaseUrl).create(IpDbApi::class.java)
        }

        val modelApi: FraudModelApi? = configuration.fraudModelBaseUrl.takeIf { it.isNotBlank() }?.let { base ->
            val client = baseOkHttpClient(configuration)
            gsonRetrofit(client, base).create(FraudModelApi::class.java)
        }

        paymentRiskClient = PaymentRiskClientImpl(ipApi, modelApi)

        scamClient = configuration.geminiApiKey?.takeIf { it.isNotBlank() }?.let { key ->
            val geminiInterceptor = headerInterceptor("x-goog-api-key") { key }
            val client = baseOkHttpClient(configuration, geminiInterceptor)
            val retrofit = gsonRetrofit(client, "https://generativelanguage.googleapis.com/")
            ScamAnalysisClientImpl(retrofit.create(GeminiRestApi::class.java))
        }

        firestoreRepository = FraudLensFirestoreRepository(FirebaseFirestore.getInstance())
    }

    @Synchronized
    fun clear() {
        config = null
        audioClient = null
        imageClient = null
        videoClient = null
        paymentRiskClient = null
        scamClient = null
        firestoreRepository = null
    }

    fun isInitialized(): Boolean = config != null

    fun audio(): AudioIntegrityClient =
        audioClient ?: error("Audio not configured: set audioBaseUrl in FraudLensConfig")

    fun image(): ImageIntegrityClient =
        imageClient ?: error("Image not configured: set imageBaseUrl in FraudLensConfig")

    fun video(): VideoIntegrityClient =
        videoClient ?: error("Video not configured: set videoBaseUrl in FraudLensConfig")

    fun paymentRisk(): PaymentRiskClient =
        paymentRiskClient ?: error("Call FraudLensSdk.initialize first")

    fun scamAnalysis(): ScamAnalysisClient =
        scamClient ?: error("Gemini not configured: set geminiApiKey in FraudLensConfig")

    fun firestoreRepository(): FraudLensFirestoreRepository =
        firestoreRepository ?: error("Call FraudLensSdk.initialize first (Firestore is created there)")

    fun firebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
