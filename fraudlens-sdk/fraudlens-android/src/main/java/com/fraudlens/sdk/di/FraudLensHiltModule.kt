package com.fraudlens.sdk.di

import com.fraudlens.sdk.FraudLensSdk
import com.fraudlens.sdk.audio.AudioIntegrityClient
import com.fraudlens.sdk.firebase.FraudLensFirestoreRepository
import com.fraudlens.sdk.image.ImageIntegrityClient
import com.fraudlens.sdk.risk.PaymentRiskClient
import com.fraudlens.sdk.scam.ScamAnalysisClient
import com.fraudlens.sdk.video.VideoIntegrityClient
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FraudLensFirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFraudLensFirestoreRepository(
        firestore: FirebaseFirestore,
    ): FraudLensFirestoreRepository = FraudLensFirestoreRepository(firestore)
}

/**
 * Delegates to [FraudLensSdk] — call [com.fraudlens.sdk.FraudLensSdk.initialize] **before** `super.onCreate()`
 * in your [@dagger.hilt.android.HiltAndroidApp] [android.app.Application] (or before any injection of these types).
 */
@Module
@InstallIn(SingletonComponent::class)
object FraudLensSdkHiltModule {

    @Provides
    fun provideAudioIntegrityClient(): AudioIntegrityClient = FraudLensSdk.audio()

    @Provides
    fun provideImageIntegrityClient(): ImageIntegrityClient = FraudLensSdk.image()

    @Provides
    fun provideVideoIntegrityClient(): VideoIntegrityClient = FraudLensSdk.video()

    @Provides
    fun providePaymentRiskClient(): PaymentRiskClient = FraudLensSdk.paymentRisk()

    @Provides
    fun provideScamAnalysisClient(): ScamAnalysisClient = FraudLensSdk.scamAnalysis()
}
