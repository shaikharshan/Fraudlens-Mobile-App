package com.example.fraudlens.demo

import com.example.fraudlens.retrofit.AbuseRiskResult
import com.example.fraudlens.retrofit.ModelOutput

enum class DemoScenarioType(val label: String) {
    NORMAL_PAYMENT("1. Normal Payment"),
    RISKY_IP_AND_LOCATION("2. Risky IP + Far-away Location"),
    ML_TRIGGERED_FRAUD("3. ML Triggered Fraud")
}

enum class DemoRunnerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    ERROR
}

data class DemoConfig(
    val payerUserId: String = "",
    val recipientVpa: String = "",
    val riskyIpForOverride: String = "",
    val farLatitude: Double = 40.7128,
    val farLongitude: Double = -74.0060,
    val normalAmount: Double = 101.0,
    val riskyAmount: Double = 9000.0,
    val mlAmount: Double = 25000.0
)

data class DemoScenarioResult(
    val scenario: DemoScenarioType,
    val statusText: String,
    val transactionStatus: String,
    val transactionIdHint: String = "",
    val ipRisk: AbuseRiskResult? = null,
    val modelOutput: ModelOutput? = null,
    val locationDeviationKm: Double? = null
)

data class DemoUiState(
    val config: DemoConfig = DemoConfig(),
    val runnerStatus: DemoRunnerStatus = DemoRunnerStatus.IDLE,
    val currentScenario: DemoScenarioType? = null,
    val awaitingNext: Boolean = false,
    val countdownSeconds: Int = 0,
    val logLines: List<String> = emptyList(),
    val results: List<DemoScenarioResult> = emptyList(),
    val latestError: String? = null
)
