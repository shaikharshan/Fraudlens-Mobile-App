package com.example.fraudlens.data.repo

import com.example.fraudlens.retrofit.GeminiAPI
import com.example.fraudlens.retrofit.GeminiRequest
import com.example.fraudlens.retrofit.GeminiResponse
import com.example.fraudlens.retrofit.GenerationConfig
import com.example.fraudlens.retrofit.ReqContent
import com.example.fraudlens.retrofit.ReqPart
import com.example.fraudlens.retrofit.ScamAnalysisResponse
import com.example.fraudlens.utils.PROMPT
import retrofit2.Response
import javax.inject.Inject
// Add this import at the top of your GeminiRepo.kt file
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException


// Represents the different possible outcomes of the analysis
sealed class ScamAnalysisResult {
    data class Success(val analysis: ScamAnalysisResponse) : ScamAnalysisResult()
    data class Error(val message: String) : ScamAnalysisResult()
    data class JsonBlob(val blob:String): ScamAnalysisResult()
    object RateLimited : ScamAnalysisResult()
    object MaxTokensReached : ScamAnalysisResult()
    object IncompleteResponse : ScamAnalysisResult()
}

class GeminiRepo @Inject constructor(
    private val geminiAPI: GeminiAPI
) {
    // A single instance of Gson can be reused
    private val gson = Gson()

    suspend fun invoke(text: String): ScamAnalysisResult {
        // The request structure you defined
        val modelRequest = GeminiRequest(
            listOf(
                ReqContent("user", listOf(ReqPart(PROMPT))),
                ReqContent("model", listOf(ReqPart("OK, I am ready. Please provide the message for analysis."))),
                ReqContent("user", listOf(ReqPart(text)))
            ),
            GenerationConfig()
        )

        return try {
            val response = geminiAPI.invoke(modelRequest)

            if (response.isSuccessful) {
                val geminiResponse = response.body()

                // Use safe calls and let blocks to avoid null pointer exceptions
                val candidate = geminiResponse?.candidates?.firstOrNull()

                if (candidate == null) {
                    return ScamAnalysisResult.IncompleteResponse
                }

                // Handle cases where the model stops because it ran out of tokens
                if (candidate.finishReason == "MAX_TOKENS") {
                    return ScamAnalysisResult.MaxTokensReached
                }

                // Get the raw text from the response part. It contains the nested JSON.
                val rawJsonText = candidate.content?.parts?.firstOrNull()?.text

                if (rawJsonText.isNullOrBlank()) {
                    return ScamAnalysisResult.IncompleteResponse
                }

                try {
                    val scamAnalysis = gson.fromJson(rawJsonText, ScamAnalysisResponse::class.java)
                    ScamAnalysisResult.Success(scamAnalysis)
                } catch (e: JsonSyntaxException) {
                    // This catches errors if the model returns malformed JSON
                    ScamAnalysisResult.JsonBlob("Failed to parse model's response: ${e.message} \n Please excuse the format error and see the reponse \n " +
                            "$rawJsonText")
                }

            } else if (response.code() == 429) {
                // Handle rate limiting specifically
                ScamAnalysisResult.RateLimited
            } else {
                // Handle other non-successful HTTP responses (e.g., 400, 500)
                ScamAnalysisResult.Error("API Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            // Handle exceptions like network errors (IOException, etc.)
            ScamAnalysisResult.Error(e.localizedMessage ?: "An unknown error occurred")
        }
    }
}