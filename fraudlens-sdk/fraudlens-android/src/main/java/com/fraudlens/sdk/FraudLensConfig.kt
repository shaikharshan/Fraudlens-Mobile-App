package com.fraudlens.sdk

/**
 * Host-supplied configuration. Use empty strings to skip a service; call the matching accessor only when configured.
 * Do not embed production keys in source.
 */
data class FraudLensConfig(
    /** Voice / deepfake HTTP API (e.g. HF Space). */
    val audioBaseUrl: String = "",
    val audioApiKey: String? = null,
    /** Image (prompt-injection / visual) API. */
    val imageBaseUrl: String = "",
    val imageApiKey: String? = null,
    /** Video analysis API. */
    val videoBaseUrl: String = "",
    val videoApiKey: String? = null,
    /** AbuseIPDB — use [abuseIpDbApiKey] with default [abuseIpDbBaseUrl]. */
    val abuseIpDbBaseUrl: String = "https://api.abuseipdb.com/api/v2/",
    val abuseIpDbApiKey: String? = null,
    /** FastAPI fraud model host (Render deployed model API). */
    val fraudModelBaseUrl: String = "https://fraudlens-updated-model-api.onrender.com/",
    /** Google AI Gemini REST (generativelanguage.googleapis.com path is added internally). */
    val geminiApiKey: String? = null,
    val enableHttpLogging: Boolean = false,
)
