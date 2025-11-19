package com.example.fraudlens.ui.components

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class FraudAnalysisResult(
    val is_scam: Boolean,
    val confidence_score: Float,
    val reasoning: String,
    val recommendation: String
)

sealed class AnalysisResult {
    data class Success(val analysis: FraudAnalysisResult, val rawResponse: String) : AnalysisResult()
    data class Error(val message: String) : AnalysisResult()
    object RateLimitExceeded : AnalysisResult()
    object NoScamDetected : AnalysisResult()
}

@Singleton
class GeminiApiManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var lastApiCallTime = 0L
    private var apiCallCount = 0

    companion object {
        private const val TAG = "GeminiApiManager"
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

        private const val FRAUD_DETECTION_PROMPT = """You are a financial scam detection expert. Analyze the speech and determine if it's a scam.

SCAM INDICATORS:
- Urgency/fear tactics (account blocked, suspended)
- Requests for OTP, PIN, passwords, CVV
- Lottery/prize claims, congratulations messages
- Impersonation (bank, police, government)
- Suspicious payment requests
- Phishing attempts

Respond ONLY with JSON:
{
  "is_scam": true/false,
  "confidence_score": 0.0-1.0,
  "reasoning": "Brief explanation in same language as input",
  "recommendation": "Action advice for user"
}"""
    }

    suspend fun analyzeAudioBatch(
        audioData: ByteArray,
        apiKey: String,
        apiCooldownMs: Long = 5000
    ): AnalysisResult = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()

        // Rate limiting check
        if (currentTime - lastApiCallTime < apiCooldownMs) {
            Log.d(TAG, "⏳ Skipping API call - cooldown period (${apiCooldownMs}ms)")
            return@withContext AnalysisResult.Error("Rate limit cooldown active")
        }

        apiCallCount++
        lastApiCallTime = currentTime

        Log.d(TAG, "📤 API Call #$apiCallCount - Audio size: ${audioData.size} bytes")

        try {
            // Convert audio to base64
            val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)

            // Build request JSON
            val requestJson = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(
                    mapOf(
                        "parts" to listOf(
                            mapOf("text" to FRAUD_DETECTION_PROMPT),
                            mapOf(
                                "inline_data" to mapOf(
                                    "mime_type" to "audio/pcm",
                                    "data" to base64Audio
                                )
                            )
                        )
                    )
                )))
                add("generationConfig", JsonObject().apply {
                    addProperty("temperature", 0.3)
                    addProperty("maxOutputTokens", 1024)
                })
            }

            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$GEMINI_API_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()

            when (response.code) {
                200 -> {
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrBlank()) {
                        return@withContext AnalysisResult.Error("Empty response from API")
                    }

                    Log.d(TAG, "✅ API Response received: ${responseBody.take(200)}...")
                    parseApiResponse(responseBody)
                }
                429 -> {
                    Log.e(TAG, "⚠️ Rate limit exceeded (429)")
                    AnalysisResult.RateLimitExceeded
                }
                else -> {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "❌ API Error: ${response.code} - $errorBody")
                    AnalysisResult.Error("API Error: ${response.code}")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Network error: ${e.message}", e)
            AnalysisResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error: ${e.message}", e)
            AnalysisResult.Error("Error: ${e.message}")
        }
    }

    private fun parseApiResponse(responseBody: String): AnalysisResult {
        return try {
            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)

            val candidates = jsonObject.getAsJsonArray("candidates")
            if (candidates == null || candidates.size() == 0) {
                return AnalysisResult.Error("No candidates in response")
            }

            val firstCandidate = candidates[0].asJsonObject
            val content = firstCandidate.getAsJsonObject("content")
            val parts = content?.getAsJsonArray("parts")

            if (parts == null || parts.size() == 0) {
                return AnalysisResult.Error("No parts in response")
            }

            val text = parts[0].asJsonObject.get("text")?.asString
            if (text.isNullOrBlank()) {
                return AnalysisResult.Error("No text in response")
            }

            Log.d(TAG, "📝 Extracted text: ${text.take(200)}...")

            // Extract JSON from response
            val jsonMatch = Regex("""\{[\s\S]*\}""").find(text)
            if (jsonMatch == null) {
                Log.w(TAG, "⚠️ No JSON found in response")
                return AnalysisResult.NoScamDetected
            }

            val analysis = gson.fromJson(jsonMatch.value, FraudAnalysisResult::class.java)

            Log.d(TAG, "🔍 Analysis: is_scam=${analysis.is_scam}, confidence=${analysis.confidence_score}")

            if (analysis.is_scam && analysis.confidence_score > 0.6f) {
                Log.d(TAG, "🚨 FRAUD DETECTED!")
                AnalysisResult.Success(analysis, text)
            } else {
                Log.d(TAG, "✅ No fraud detected or low confidence")
                AnalysisResult.NoScamDetected
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing response: ${e.message}", e)
            AnalysisResult.Error("Failed to parse response: ${e.message}")
        }
    }

    fun getApiCallCount(): Int = apiCallCount

    fun resetApiCallCount() {
        apiCallCount = 0
    }
}