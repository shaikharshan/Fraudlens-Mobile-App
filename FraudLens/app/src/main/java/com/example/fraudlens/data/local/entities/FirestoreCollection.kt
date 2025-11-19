package com.example.fraudlens.data.local.entities



enum class FirestoreCollection(val value: String){
    USER("users"),
    TRANSACTIONS("transactions"),
    DEVICE("devices"),
    LOCATION("location_logs"),
    IP("ip_logs")
}

enum class TransactionResponse(val value: String){
    APPROVED("approved"),
    PENDING("pending"),
    BLOCKED("blocked")
}
enum class  Risk(val value: String){
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW")
}

const val DEVIATION = 100.0
const val FLAG_COUNTDOWN_SEC = 15