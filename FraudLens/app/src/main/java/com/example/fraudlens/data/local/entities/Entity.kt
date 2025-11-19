//package com.example.fraudlens.data.local.entities
//
//import androidx.room.ColumnInfo
//import androidx.room.Embedded
//import androidx.room.Entity
//import androidx.room.ForeignKey
//import androidx.room.PrimaryKey
//import androidx.room.Relation
//
//@Entity(tableName = "user")
//data class User(
//    @PrimaryKey(autoGenerate = true)
//    @ColumnInfo(name = "userId")
//    var userId: Long=0,
//
//    var username: String,
//    var email: String,
//    var password: String,
//
//    @ColumnInfo(name = "phone")
//    var phone : String = "",
//    val registeredAt: Long = System.currentTimeMillis(),
//    val bankVPA: String = "",
//    val bankIFSC:String = "",
//    var balance: Double= 0.0,
//    val biometricEnabled: Boolean = false,
//
//    )
//
//
//
//@Entity(tableName = "transactions")
//data class Transactions(
//    @PrimaryKey(autoGenerate = true) val transactionId: Long = 0,
//
//    val payerUserId: Long,       // Link to User table
//    val payerDeviceId: String,   // Match DeviceInfo.deviceId
//
//    val receiverUserId:Long,
//    val receiverVpa: String,     // e.g., friend@upi
//    val receiverIfsc: String,    // e.g., HDFC0001234
//
//    val amount: Double,
//    val timestamp: Long = System.currentTimeMillis(),
//    val status: String, // "PENDING", "APPROVED", "BLOCKED"
//
//
//    // Fraud Evaluation
//    val fraudScore: Float= 0F,
//    val ipRiskScore: Float = 0F,
//    val locationRiskScore: Float = 0F,
//    val modelDecision: Boolean = false  // true = block(fraud), false = Not fraud
//)
//
//@Entity(tableName = "ip_log")
//data class IPLog(
//    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val transactionId: Long,
//    val ipAddress: String,
//    val riskScore: Float,
//    val isBlocked: Boolean,
//    val country: String?,
//    val isp: String?
//)
//
//
//@Entity(tableName = "location_logs")
//data class LocationLog(
//    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val transactionId: Long,
//    val latitude: Double,
//    val longitude: Double,
//    val deviationFromLast: Float?,
//    val isSuspicious: Boolean
//)
//
//@Entity(tableName = "device_info")
//data class DeviceInfo(
//    @PrimaryKey(autoGenerate = true) val id: Long = 0,
//    val userId: Long,
//    val deviceId: String,     // Unique device ID (e.g., UUID or Android ID)
//    val deviceModel: String,
//    val osVersion: String,
//    val isRooted: Boolean,
//    val lastActive: Long = System.currentTimeMillis()
//)
//
//data class UserWithDevices(
//    @Embedded val user: User,
//    @Relation(
//        parentColumn = "userId",
//        entityColumn = "userId"
//    )
//    val devices: List<DeviceInfo>
//)
//
//data class UserWithTransactions(
//    @Embedded val user: User,
//    @Relation(
//        parentColumn = "userId",
//        entityColumn = "payerUserId"
//    )
//    val transactions: List<Transactions>
//)
//
//data class TransactionAndIPLog(
//    @Embedded val transaction: Transactions,
//    @Relation(
//        parentColumn = "transactionId",
//        entityColumn = "transactionId"
//    )
//    val ipLog: IPLog
//)
//
//data class TransactionAndLocationLog(
//    @Embedded val transaction: Transactions,
//    @Relation(
//        parentColumn = "transactionId",
//        entityColumn = "transactionId"
//    )
//    val locationLog: LocationLog
//)
//
