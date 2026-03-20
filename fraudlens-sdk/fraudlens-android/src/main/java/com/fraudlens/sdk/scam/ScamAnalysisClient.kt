package com.fraudlens.sdk.scam

import com.fraudlens.sdk.internal.scam.GeminiRestApi
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ScamAnalysisResult {
    data class Success(val analysis: ScamAnalysisResponse) : ScamAnalysisResult()
    data class Error(val message: String) : ScamAnalysisResult()
    data class RawText(val text: String) : ScamAnalysisResult()
    data object RateLimited : ScamAnalysisResult()
}

interface ScamAnalysisClient {
    suspend fun analyzeMessage(combinedText: String): ScamAnalysisResult
}

internal class ScamAnalysisClientImpl(
    private val api: GeminiRestApi,
) : ScamAnalysisClient {

    private val gson = Gson()

    override suspend fun analyzeMessage(combinedText: String): ScamAnalysisResult = withContext(Dispatchers.IO) {
        val modelRequest = GeminiRequest(
            contents = listOf(
                ReqContent("user", listOf(ReqPart(DEFAULT_SCAM_ANALYSIS_PROMPT))),
                ReqContent("model", listOf(ReqPart("OK, I am ready. Please provide the message for analysis."))),
                ReqContent("user", listOf(ReqPart(combinedText))),
            ),
            generationConfig = GenerationConfig(),
        )
        try {
            val response = api.generateContent(modelRequest)
            if (response.code() == 429) return@withContext ScamAnalysisResult.RateLimited
            if (!response.isSuccessful) {
                return@withContext ScamAnalysisResult.Error("API ${response.code()}: ${response.message()}")
            }
            val geminiResponse = response.body()
            val candidate = geminiResponse?.candidates?.firstOrNull()
                ?: return@withContext ScamAnalysisResult.Error("Incomplete response")
            if (candidate.finishReason == "MAX_TOKENS") {
                return@withContext ScamAnalysisResult.Error("Max tokens reached")
            }
            val rawJsonText = candidate.content?.parts?.firstOrNull()?.text
                ?: return@withContext ScamAnalysisResult.Error("Empty model text")
            return@withContext try {
                ScamAnalysisResult.Success(gson.fromJson(rawJsonText, ScamAnalysisResponse::class.java))
            } catch (_: JsonSyntaxException) {
                ScamAnalysisResult.RawText(rawJsonText)
            }
        } catch (e: Exception) {
            ScamAnalysisResult.Error(e.message ?: "Unknown error")
        }
    }
}
