package com.fraudlens.sdk.firebase

import com.google.firebase.Timestamp

enum class FirestoreCollection(val value: String) {
    USER("users"),
    TRANSACTIONS("transactions"),
    DEVICE("devices"),
    LOCATION("location_logs"),
    IP("ip_logs"),
}

data class FirestoreUser(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val registeredAt: Long = System.currentTimeMillis(),
    val bankVPA: String = "",
    val bankIFSC: String = "",
    val balance: Double = 0.0,
    val biometricEnabled: Boolean = false,
)

data class FirestoreTransactions(
    val payerUserId: String = "",
    val payerDeviceId: String = "",
    val payerIFSC: String = "",
    val payerVpa: String = "",
    val receiverUserId: String = "",
    val receiverVpa: String = "",
    val receiverIfsc: String = "",
    val amount: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "PENDING",
    val locationLogId: String = "",
    val ipLogId: String = "",
    val fraudScore: Float = 0F,
    val ipRiskScore: Float = 0F,
    val locationRiskScore: Float = 0F,
    val modelDecision: Boolean = false,
)
