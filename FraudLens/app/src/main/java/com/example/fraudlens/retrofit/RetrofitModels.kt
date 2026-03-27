package com.example.fraudlens.retrofit

import kotlin.compareTo
import kotlinx.serialization.Serializable

data class AbuseIPCheckResponse(
    val data: AbuseIPData
)

data class AbuseIPData(
    val ipAddress: String,
    val isPublic: Boolean,
    val ipVersion: Int,
    val isWhitelisted: Boolean,
    val abuseConfidenceScore: Int,
    val countryCode: String?,
    val countryName: String?,
    val usageType: String?,
    val isp: String?,
    val domain: String?,
    val isTor: Boolean,
    val totalReports: Int,
    val numDistinctUsers: Int,
    val lastReportedAt: String?
)

data class AbuseIPReportResponse(
    val data: AbuseReportData
)

data class AbuseReportData(
    val totalReports: Int,
    val results: List<AbuseReport>
)

data class AbuseReport(
    val reportedAt: String,
    val comment: String?,
    val categories: List<Int>,
    val reporterCountryName: String?
)

data class AbuseRiskResult(
    val ip:String,
    val isRisky: Boolean,
    val abuseConfidenceScore: Int,
    val reasons: List<String>,
    val country: String,
    val isp: String
)



data class ModelInput(
    val txn_id: String,
    val AMOUNT: Double,
    val amount_sum_1h: Double,
    val TXN_TIMESTAMP: String,
    val PAYER_VPA: String,
    val BENEFICIARY_VPA: String,
    val PAYER_IFSC: String,
    val BENEFICIARY_IFSC: String,
    val INITIATION_MODE: String = "APP",
    val TRANSACTION_TYPE: String = "P2P",
    val device_user_count: Int,
    val txn_count_1h: Int,
    val failed_txn_count_24h: Int,
    val consecutive_failures: Int
)

data class ModelOutput(
    val txn_id: String,
    val is_fraud: Boolean = false,
    val fraud_probability: Float = 0.0f,
    val risk_level: String = "LOW"
)

data class ModelHealthOutput(
    val status :String,
    val model_loaded: Boolean,
    val model_path:String
)


object IPAbuseRiskHelper {
    fun evaluateRisk(data: AbuseIPData): AbuseRiskResult {

        val reasons = mutableListOf<String>()
        val checkList = "data center"+" web hosting"+" transit"

        if (data.abuseConfidenceScore >= 75) {
            reasons.add("High abuse confidence score: ${data.abuseConfidenceScore}%")
        }
        if (data.isTor) {
            reasons.add("Connection via Tor network")
        }
        if (checkList.contains(data.usageType.toString(), ignoreCase = true)) {
            reasons.add("Usage type is ${data.usageType}")
        }
        if (data.totalReports > 10 && data.numDistinctUsers > 3) {
            reasons.add("Multiple reports by different users")
        }

        val isRisky = reasons.isNotEmpty()

        return AbuseRiskResult(
            ip = data.ipAddress,
            isRisky = isRisky,
            reasons = reasons,
            country = data.countryName ?: "Unknown",
            isp = data.isp ?: "Unknown",
            abuseConfidenceScore = data.abuseConfidenceScore
        )
    }
}

//Gemini API
//Request
data class GeminiRequest(
    val contents: List<ReqContent>,
    val generationConfig: GenerationConfig
)

data class ReqContent(
    val role: String,
    val parts: List<ReqPart>
)

data class ReqPart(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.2f,
    val topP: Float = 0.8f,
    val topK: Int = 40,
    val maxOutputTokens: Int = 1024,
    val response_mime_type: String = "application/json"
)

//Gemini Response
data class ScamAnalysisResponse(
    val is_scam: Boolean,
    val confidence_score: Float,
    val reasoning: String,
    val recommendation: String
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?
)

data class Content(
    val parts: List<Part>?,
    val role: String?
)

data class Part(
    val text: String?
)

//razorpay
data class RazorpayOrderRequest(
    val amount: Int, // Amount in paise (100 paise = 1 INR)
    val currency: String = "INR",
    val receipt: String,
    val notes: Map<String, String>? = null
)

data class RazorpayOrderResponse(
    val id: String,
    val entity: String,
    val amount: Int,
    val amount_paid: Int,
    val amount_due: Int,
    val currency: String,
    val receipt: String,
    val status: String,
    val attempts: Int,
    val notes: Map<String, String>?,
    val created_at: Long
)

data class RazorpayPaymentResult(
    val orderId: String,
    val paymentId: String?,
    val signature: String?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

