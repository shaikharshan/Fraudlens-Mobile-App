package com.fraudlens.reactnative

import android.util.Base64
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.fraudlens.sdk.FraudLensConfig
import com.fraudlens.sdk.FraudLensSdk
import com.fraudlens.sdk.audio.model.VoiceDetectionRequest
import com.fraudlens.sdk.image.model.MediaUploadRequest
import com.fraudlens.sdk.risk.ModelInput
import com.fraudlens.sdk.scam.ScamAnalysisResult
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FraudLensModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    override fun getName(): String = "FraudLens"

    @ReactMethod
    fun initialize(config: ReadableMap, promise: Promise) {
        try {
            FraudLensSdk.initialize(parseConfig(config))
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("INIT", e.message, e)
        }
    }

    @ReactMethod
    fun clear(promise: Promise) {
        try {
            FraudLensSdk.clear()
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("CLEAR", e.message, e)
        }
    }

    @ReactMethod
    fun audioHealth(promise: Promise) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { FraudLensSdk.audio().health().getOrThrow() }
            }.onSuccess { promise.resolve(it) }
                .onFailure { promise.reject("AUDIO", it.message, it) }
        }
    }

    @ReactMethod
    fun audioDetectBase64(payload: ReadableMap, promise: Promise) {
        scope.launch {
            runCatching {
                val bytes = decodeBase64Payload(payload)
                val req = VoiceDetectionRequest(
                    audioBytes = bytes,
                    filename = payload.getString("filename")!!,
                    contentType = payload.getString("contentType")!!,
                    partName = if (payload.hasKey("partName")) payload.getString("partName")!! else "file",
                )
                withContext(Dispatchers.IO) {
                    FraudLensSdk.audio().detectVoice(req).getOrThrow()
                }
            }.onSuccess { promise.resolve(it) }
                .onFailure { promise.reject("AUDIO", it.message, it) }
        }
    }

    @ReactMethod
    fun imageHealth(promise: Promise) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { FraudLensSdk.image().health().getOrThrow() }
            }.onSuccess { promise.resolve(it) }
                .onFailure { promise.reject("IMAGE", it.message, it) }
        }
    }

    @ReactMethod
    fun imageDetectBase64(payload: ReadableMap, promise: Promise) {
        scope.launch {
            runCatching {
                val bytes = decodeBase64Payload(payload)
                val req = MediaUploadRequest(
                    bytes = bytes,
                    filename = payload.getString("filename")!!,
                    contentType = payload.getString("contentType")!!,
                    partName = if (payload.hasKey("partName")) payload.getString("partName")!! else "file",
                )
                withContext(Dispatchers.IO) {
                    FraudLensSdk.image().detectImage(req).getOrThrow()
                }
            }.onSuccess { promise.resolve(it) }
                .onFailure { promise.reject("IMAGE", it.message, it) }
        }
    }

    @ReactMethod
    fun videoHealth(promise: Promise) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) { FraudLensSdk.video().health().getOrThrow() }
            }.onSuccess { promise.resolve(it) }
                .onFailure { promise.reject("VIDEO", it.message, it) }
        }
    }

    @ReactMethod
    fun videoDetectBase64(payload: ReadableMap, promise: Promise) {
        scope.launch {
            runCatching {
                val bytes = decodeBase64Payload(payload)
                val req = MediaUploadRequest(
                    bytes = bytes,
                    filename = payload.getString("filename")!!,
                    contentType = payload.getString("contentType")!!,
                    partName = if (payload.hasKey("partName")) payload.getString("partName")!! else "file",
                )
                withContext(Dispatchers.IO) {
                    FraudLensSdk.video().detectVideo(req).getOrThrow()
                }
            }.onSuccess { promise.resolve(it) }
                .onFailure { promise.reject("VIDEO", it.message, it) }
        }
    }

    @ReactMethod
    fun checkIpReputation(ip: String, promise: Promise) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    FraudLensSdk.paymentRisk().checkIpReputation(ip).getOrThrow()
                }
            }.onSuccess { r ->
                val map = com.facebook.react.bridge.Arguments.createMap()
                map.putString("ip", r.ip)
                map.putBoolean("isRisky", r.isRisky)
                map.putInt("abuseConfidenceScore", r.abuseConfidenceScore)
                map.putString("country", r.country)
                map.putString("isp", r.isp)
                val arr = com.facebook.react.bridge.Arguments.createArray()
                r.reasons.forEach { arr.pushString(it) }
                map.putArray("reasons", arr)
                promise.resolve(map)
            }.onFailure { promise.reject("IP", it.message, it) }
        }
    }

    @ReactMethod
    fun checkModelHealth(promise: Promise) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    FraudLensSdk.paymentRisk().checkModelHealth().getOrThrow()
                }
            }.onSuccess { h ->
                val map = com.facebook.react.bridge.Arguments.createMap()
                map.putString("status", h.status)
                map.putBoolean("model_loaded", h.model_loaded)
                promise.resolve(map)
            }.onFailure { promise.reject("MODEL", it.message, it) }
        }
    }

    @ReactMethod
    fun predictFraud(input: ReadableMap, promise: Promise) {
        scope.launch {
            runCatching {
                val json = gson.toJson(readableMapToMap(input))
                val modelInput = gson.fromJson(json, ModelInput::class.java)
                withContext(Dispatchers.IO) {
                    FraudLensSdk.paymentRisk().predictFraud(modelInput).getOrThrow()
                }
            }.onSuccess { o ->
                val map = com.facebook.react.bridge.Arguments.createMap()
                map.putString("txn_id", o.txn_id)
                map.putBoolean("is_fraud", o.is_fraud)
                map.putDouble("fraud_probability", o.fraud_probability.toDouble())
                map.putString("risk_level", o.risk_level)
                promise.resolve(map)
            }.onFailure { promise.reject("MODEL", it.message, it) }
        }
    }

    @ReactMethod
    fun analyzeScam(combinedText: String, promise: Promise) {
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    FraudLensSdk.scamAnalysis().analyzeMessage(combinedText)
                }
            }.onSuccess { r ->
                promise.resolve(scamToWritableMap(r))
            }.onFailure { promise.reject("SCAM", it.message, it) }
        }
    }

    private fun decodeBase64Payload(payload: ReadableMap): ByteArray {
        val b64 = payload.getString("base64") ?: error("base64 required")
        return Base64.decode(b64, Base64.DEFAULT)
    }

    private fun scamToWritableMap(r: ScamAnalysisResult): com.facebook.react.bridge.WritableMap {
        val m = com.facebook.react.bridge.Arguments.createMap()
        when (r) {
            is ScamAnalysisResult.Success -> {
                m.putString("type", "success")
                m.putBoolean("is_scam", r.analysis.is_scam)
                m.putDouble("confidence_score", r.analysis.confidence_score.toDouble())
                m.putString("reasoning", r.analysis.reasoning)
                m.putString("recommendation", r.analysis.recommendation)
            }
            is ScamAnalysisResult.Error -> {
                m.putString("type", "error")
                m.putString("message", r.message)
            }
            is ScamAnalysisResult.RawText -> {
                m.putString("type", "raw")
                m.putString("text", r.text)
            }
            ScamAnalysisResult.RateLimited -> m.putString("type", "rate_limited")
        }
        return m
    }

    private fun parseConfig(m: ReadableMap): FraudLensConfig {
        fun str(key: String): String = if (m.hasKey(key) && !m.isNull(key)) m.getString(key) ?: "" else ""
        fun strOrNull(key: String): String? =
            if (m.hasKey(key) && !m.isNull(key)) m.getString(key) else null
        val abuseBase = str("abuseIpDbBaseUrl")
        return FraudLensConfig(
            audioBaseUrl = str("audioBaseUrl"),
            audioApiKey = strOrNull("audioApiKey"),
            imageBaseUrl = str("imageBaseUrl"),
            imageApiKey = strOrNull("imageApiKey"),
            videoBaseUrl = str("videoBaseUrl"),
            videoApiKey = strOrNull("videoApiKey"),
            abuseIpDbBaseUrl = abuseBase.ifBlank { "https://api.abuseipdb.com/api/v2/" },
            abuseIpDbApiKey = strOrNull("abuseIpDbApiKey"),
            fraudModelBaseUrl = str("fraudModelBaseUrl"),
            geminiApiKey = strOrNull("geminiApiKey"),
            enableHttpLogging = m.hasKey("enableHttpLogging") && !m.isNull("enableHttpLogging") && m.getBoolean("enableHttpLogging"),
        )
    }

    private fun readableMapToMap(readableMap: ReadableMap): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val it = readableMap.keySetIterator()
        while (it.hasNextKey()) {
            val key = it.nextKey()
            when (readableMap.getType(key)) {
                ReadableType.Null -> map[key] = null
                ReadableType.Boolean -> map[key] = readableMap.getBoolean(key)
                ReadableType.Number -> {
                    val d = readableMap.getDouble(key)
                    map[key] = if (kotlin.math.abs(d % 1.0) < 1e-9) d.toInt() else d
                }
                ReadableType.String -> map[key] = readableMap.getString(key)
                ReadableType.Map -> map[key] = readableMapToMap(readableMap.getMap(key)!!)
                else -> { /* Array omitted for ModelInput */ }
            }
        }
        return map
    }
}
