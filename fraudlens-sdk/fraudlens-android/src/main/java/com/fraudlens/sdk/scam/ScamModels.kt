package com.fraudlens.sdk.scam

data class GeminiRequest(
    val contents: List<ReqContent>,
    val generationConfig: GenerationConfig,
)

data class ReqContent(
    val role: String,
    val parts: List<ReqPart>,
)

data class ReqPart(
    val text: String,
)

data class GenerationConfig(
    val temperature: Float = 0.2f,
    val topP: Float = 0.8f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 1024,
    val response_mime_type: String = "application/json",
)

data class ScamAnalysisResponse(
    val is_scam: Boolean,
    val confidence_score: Float,
    val reasoning: String,
    val recommendation: String,
)

data class GeminiResponse(
    val candidates: List<Candidate>?,
)

data class Candidate(
    val content: Content?,
    val finishReason: String?,
)

data class Content(
    val parts: List<Part>?,
    val role: String?,
)

data class Part(
    val text: String?,
)
