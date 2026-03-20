package com.fraudlens.fraudlens_flutter

import com.fraudlens.sdk.FraudLensConfig
import com.fraudlens.sdk.FraudLensSdk
import com.fraudlens.sdk.audio.model.VoiceDetectionRequest
import com.fraudlens.sdk.image.model.MediaUploadRequest
import com.fraudlens.sdk.risk.ModelInput
import com.fraudlens.sdk.scam.ScamAnalysisResult
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FraudlensFlutterPlugin : FlutterPlugin, MethodCallHandler {

    private lateinit var channel: MethodChannel
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val gson = Gson()

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "fraudlens_flutter")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> {
                try {
                    FraudLensSdk.initialize(parseConfig(call.arguments))
                    result.success(null)
                } catch (e: Exception) {
                    result.error("INIT", e.message, null)
                }
            }
            "clear" -> {
                FraudLensSdk.clear()
                result.success(null)
            }
            "audioHealth" -> scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { FraudLensSdk.audio().health().getOrThrow() }
                }.onSuccess { result.success(it) }
                    .onFailure { result.error("AUDIO", it.message, null) }
            }
            "audioDetect" -> scope.launch {
                runCatching {
                    val m = call.arguments as Map<*, *>
                    val bytes = (m["bytes"] as ByteArray)
                    val req = VoiceDetectionRequest(
                        audioBytes = bytes,
                        filename = m["filename"] as String,
                        contentType = m["contentType"] as String,
                        partName = m["partName"] as? String ?: "file",
                    )
                    withContext(Dispatchers.IO) {
                        FraudLensSdk.audio().detectVoice(req).getOrThrow()
                    }
                }.onSuccess { result.success(it) }
                    .onFailure { result.error("AUDIO", it.message, null) }
            }
            "imageHealth" -> scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { FraudLensSdk.image().health().getOrThrow() }
                }.onSuccess { result.success(it) }
                    .onFailure { result.error("IMAGE", it.message, null) }
            }
            "imageDetect" -> scope.launch {
                runCatching {
                    val m = call.arguments as Map<*, *>
                    val req = MediaUploadRequest(
                        bytes = m["bytes"] as ByteArray,
                        filename = m["filename"] as String,
                        contentType = m["contentType"] as String,
                        partName = m["partName"] as? String ?: "file",
                    )
                    withContext(Dispatchers.IO) {
                        FraudLensSdk.image().detectImage(req).getOrThrow()
                    }
                }.onSuccess { result.success(it) }
                    .onFailure { result.error("IMAGE", it.message, null) }
            }
            "videoHealth" -> scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) { FraudLensSdk.video().health().getOrThrow() }
                }.onSuccess { result.success(it) }
                    .onFailure { result.error("VIDEO", it.message, null) }
            }
            "videoDetect" -> scope.launch {
                runCatching {
                    val m = call.arguments as Map<*, *>
                    val req = MediaUploadRequest(
                        bytes = m["bytes"] as ByteArray,
                        filename = m["filename"] as String,
                        contentType = m["contentType"] as String,
                        partName = m["partName"] as? String ?: "file",
                    )
                    withContext(Dispatchers.IO) {
                        FraudLensSdk.video().detectVideo(req).getOrThrow()
                    }
                }.onSuccess { result.success(it) }
                    .onFailure { result.error("VIDEO", it.message, null) }
            }
            "checkIpReputation" -> scope.launch {
                val ip = call.arguments as String
                runCatching {
                    withContext(Dispatchers.IO) {
                        FraudLensSdk.paymentRisk().checkIpReputation(ip).getOrThrow()
                    }
                }.onSuccess { r ->
                    result.success(
                        mapOf(
                            "ip" to r.ip,
                            "isRisky" to r.isRisky,
                            "abuseConfidenceScore" to r.abuseConfidenceScore,
                            "reasons" to r.reasons,
                            "country" to r.country,
                            "isp" to r.isp,
                        ),
                    )
                }.onFailure { result.error("IP", it.message, null) }
            }
            "checkModelHealth" -> scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        FraudLensSdk.paymentRisk().checkModelHealth().getOrThrow()
                    }
                }.onSuccess { h ->
                    result.success(mapOf("status" to h.status, "model_loaded" to h.model_loaded))
                }.onFailure { result.error("MODEL", it.message, null) }
            }
            "predictFraud" -> scope.launch {
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    val m = call.arguments as Map<String, Any?>
                    val input = gson.fromJson(gson.toJson(m), ModelInput::class.java)
                    withContext(Dispatchers.IO) {
                        FraudLensSdk.paymentRisk().predictFraud(input).getOrThrow()
                    }
                }.onSuccess { o ->
                    result.success(
                        mapOf(
                            "txn_id" to o.txn_id,
                            "is_fraud" to o.is_fraud,
                            "fraud_probability" to o.fraud_probability,
                            "risk_level" to o.risk_level,
                        ),
                    )
                }.onFailure { result.error("MODEL", it.message, null) }
            }
            "analyzeScam" -> scope.launch {
                val text = call.arguments as String
                runCatching {
                    withContext(Dispatchers.IO) {
                        FraudLensSdk.scamAnalysis().analyzeMessage(text)
                    }
                }.onSuccess { r ->
                    result.success(scamToMap(r))
                }.onFailure { result.error("SCAM", it.message, null) }
            }
            else -> result.notImplemented()
        }
    }

    private fun scamToMap(r: ScamAnalysisResult): Map<String, Any?> =
        when (r) {
            is ScamAnalysisResult.Success -> mapOf(
                "type" to "success",
                "is_scam" to r.analysis.is_scam,
                "confidence_score" to r.analysis.confidence_score,
                "reasoning" to r.analysis.reasoning,
                "recommendation" to r.analysis.recommendation,
            )
            is ScamAnalysisResult.Error -> mapOf("type" to "error", "message" to r.message)
            is ScamAnalysisResult.RawText -> mapOf("type" to "raw", "text" to r.text)
            ScamAnalysisResult.RateLimited -> mapOf("type" to "rate_limited")
        }

    private fun parseConfig(arguments: Any?): FraudLensConfig {
        @Suppress("UNCHECKED_CAST")
        val m = arguments as? Map<String, Any?> ?: emptyMap()
        fun str(key: String) = m[key] as? String ?: ""
        fun strOrNull(key: String) = m[key] as? String
        return FraudLensConfig(
            audioBaseUrl = str("audioBaseUrl"),
            audioApiKey = strOrNull("audioApiKey"),
            imageBaseUrl = str("imageBaseUrl"),
            imageApiKey = strOrNull("imageApiKey"),
            videoBaseUrl = str("videoBaseUrl"),
            videoApiKey = strOrNull("videoApiKey"),
            abuseIpDbBaseUrl = str("abuseIpDbBaseUrl").ifBlank { "https://api.abuseipdb.com/api/v2/" },
            abuseIpDbApiKey = strOrNull("abuseIpDbApiKey"),
            fraudModelBaseUrl = str("fraudModelBaseUrl"),
            geminiApiKey = strOrNull("geminiApiKey"),
            enableHttpLogging = m["enableHttpLogging"] as? Boolean ?: false,
        )
    }
}
