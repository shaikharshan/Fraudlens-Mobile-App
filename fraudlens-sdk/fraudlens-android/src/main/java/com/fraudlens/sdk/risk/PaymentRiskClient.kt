package com.fraudlens.sdk.risk

import com.fraudlens.sdk.internal.risk.FraudModelApi
import com.fraudlens.sdk.internal.risk.IpDbApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AbuseIPDB + hosted fraud model (FastAPI-style) used for pre-payment checks.
 */
interface PaymentRiskClient {
    suspend fun checkIpReputation(ip: String): Result<AbuseRiskResult>
    suspend fun checkModelHealth(): Result<ModelHealthOutput>
    suspend fun predictFraud(input: ModelInput): Result<ModelOutput>
}

internal class PaymentRiskClientImpl(
    private val ipApi: IpDbApi?,
    private val modelApi: FraudModelApi?,
) : PaymentRiskClient {

    override suspend fun checkIpReputation(ip: String): Result<AbuseRiskResult> = withContext(Dispatchers.IO) {
        runCatching {
            val api = ipApi ?: error("AbuseIPDB is not configured (set abuseIpDbApiKey and abuseIpDbBaseUrl)")
            val res = api.checkIP(ip)
            if (!res.isSuccessful) {
                error("AbuseIPDB HTTP ${res.code()}: ${res.message()}")
            }
            val data = res.body()?.data ?: error("Empty AbuseIPDB body")
            IPAbuseRiskHelper.evaluateRisk(data)
        }
    }

    override suspend fun checkModelHealth(): Result<ModelHealthOutput> = withContext(Dispatchers.IO) {
        runCatching {
            val api = modelApi ?: error("Fraud model is not configured (set fraudModelBaseUrl)")
            val res = api.checkHealth()
            if (!res.isSuccessful) {
                error("Model health HTTP ${res.code()}: ${res.message()}")
            }
            res.body() ?: error("Empty model health body")
        }
    }

    override suspend fun predictFraud(input: ModelInput): Result<ModelOutput> = withContext(Dispatchers.IO) {
        runCatching {
            val api = modelApi ?: error("Fraud model is not configured (set fraudModelBaseUrl)")
            val res = api.predictFraud(input)
            if (!res.isSuccessful) {
                error("predict HTTP ${res.code()}: ${res.message()}")
            }
            res.body() ?: error("Empty predict body")
        }
    }
}
