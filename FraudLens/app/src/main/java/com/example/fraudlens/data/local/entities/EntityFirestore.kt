package com.example.fraudlens.data.local.entities

import com.google.firebase.Timestamp

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
    val biometricEnabled: Boolean = false
)
data class FirestoreDeviceInfo(
    val userId:String = "",
    val deviceId: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    @field:JvmField
    val isRooted: Boolean = false,
    val lastActive: Long = System.currentTimeMillis()
)
data class FirestoreTransactions(
    val payerUserId: String = "",
    val payerDeviceId: String = "",
    val payerIFSC: String="",
    val payerVpa: String = "",

    val receiverUserId: String="",
    val receiverVpa: String = "",
    val receiverIfsc: String = "",

    val amount: Double = 0.0,
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = "PENDING", // "PENDING", "APPROVED", "BLOCKED"
    val locationLogId: String="",
    val ipLogId: String="",

    val fraudScore: Float = 0F,
    val ipRiskScore: Float = 0F,
    val locationRiskScore: Float = 0F,
    val modelDecision: Boolean = false
)
data class FirestoreIPLog(
    val ipAddress: String = "",
    val riskScore: Float = 0F,
    val isBlocked: Boolean = false,
    val country: String? = null,
    val isp: String? = null
)
data class FirestoreLocationLog(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val deviationFromLast: Double = 0.0,
    val isSuspicious: Boolean = false
)
