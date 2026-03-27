package com.example.fraudlens.module

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RazorpayRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RazorpayClient