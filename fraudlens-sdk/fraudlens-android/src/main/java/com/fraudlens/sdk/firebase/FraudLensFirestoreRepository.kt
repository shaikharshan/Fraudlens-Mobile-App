package com.fraudlens.sdk.firebase

import com.fraudlens.sdk.risk.ModelInput
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Thin Firestore helpers aligned with the FraudLens demo schema.
 * The host app must apply `google-services` and initialize Firebase before use.
 */
class FraudLensFirestoreRepository(
    private val firestore: FirebaseFirestore,
) {
    data class RiskFeatureStats(
        val deviceUserCount: Int,
        val txnCount1h: Int,
        val amountSum1h: Double,
        val failedTxnCount24h: Int,
        val consecutiveFailures: Int,
    )

    private fun isBlockedStatus(status: String?): Boolean = status?.equals("blocked", ignoreCase = true) == true

    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirestoreUser> =
        withContext(Dispatchers.IO) {
            runCatching {
                val snap = firestore.collection(FirestoreCollection.USER.value)
                    .whereEqualTo("email", email)
                    .get()
                    .await()
                val doc = snap.documents.firstOrNull()
                    ?: error("Invalid credentials")
                val user = doc.toObject(FirestoreUser::class.java) ?: error("Invalid user document")
                if (user.password != password) error("Invalid credentials")
                user
            }
        }

    suspend fun loadTransactionsForUser(userId: String): Result<List<FirestoreTransactions>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val sent = firestore.collection(FirestoreCollection.TRANSACTIONS.value)
                    .whereEqualTo("payerUserId", userId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(FirestoreTransactions::class.java) }
                val recv = firestore.collection(FirestoreCollection.TRANSACTIONS.value)
                    .whereEqualTo("receiverUserId", userId)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(FirestoreTransactions::class.java) }
                (sent + recv).sortedByDescending { it.timestamp }
            }
        }

    suspend fun searchUsersByVpaPrefix(prefix: String): Result<List<FirestoreUser>> =
        withContext(Dispatchers.IO) {
            runCatching {
                firestore.collection(FirestoreCollection.USER.value)
                    .orderBy("bankVPA")
                    .startAt(prefix)
                    .endAt(prefix + "\uf8ff")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(FirestoreUser::class.java) }
            }
        }

    suspend fun computeRiskFeatureStats(userId: String): Result<RiskFeatureStats> =
        withContext(Dispatchers.IO) {
            runCatching {
                val deviceUserCount = firestore.collection(FirestoreCollection.USER.value)
                    .document(userId)
                    .collection(FirestoreCollection.DEVICE.value)
                    .get()
                    .await()
                    .size()

                val oneHourAgo = Timestamp(Date(System.currentTimeMillis() - (3600 * 1000)))
                val txnSnapshot1h = firestore.collection(FirestoreCollection.TRANSACTIONS.value)
                    .whereEqualTo("payerUserId", userId)
                    .whereGreaterThan("timestamp", oneHourAgo)
                    .get()
                    .await()
                val payerTxns1h = txnSnapshot1h
                    .documents
                    .mapNotNull { it.toObject(FirestoreTransactions::class.java) }
                val txnCount1h = payerTxns1h.size
                val amountSum1h = payerTxns1h.sumOf { txn -> txn.amount }

                val twentyFourHoursAgo = Timestamp(Date(System.currentTimeMillis() - (24 * 3600 * 1000)))
                val payerTxnsLast24h = firestore.collection(FirestoreCollection.TRANSACTIONS.value)
                    .whereEqualTo("payerUserId", userId)
                    .whereGreaterThan("timestamp", twentyFourHoursAgo)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(FirestoreTransactions::class.java) }
                val failedTxnCount24h = payerTxnsLast24h.count { txn -> isBlockedStatus(txn.status) }

                val payerTxnsLatestFirst = firestore.collection(FirestoreCollection.TRANSACTIONS.value)
                    .whereEqualTo("payerUserId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.toObject(FirestoreTransactions::class.java) }

                var consecutiveFailures = 0
                for (txn in payerTxnsLatestFirst) {
                    if (isBlockedStatus(txn.status)) {
                        consecutiveFailures += 1
                    } else {
                        break
                    }
                }

                RiskFeatureStats(
                    deviceUserCount = deviceUserCount,
                    txnCount1h = txnCount1h,
                    amountSum1h = amountSum1h,
                    failedTxnCount24h = failedTxnCount24h,
                    consecutiveFailures = consecutiveFailures,
                )
            }
        }

    suspend fun prepareModelInput(
        currentUser: FirestoreUser,
        receiver: FirestoreUser,
        amount: Double,
        initiationMode: String = "APP",
        transactionType: String = "P2P",
    ): Result<ModelInput> = withContext(Dispatchers.IO) {
        runCatching {
            val featureStats = computeRiskFeatureStats(currentUser.userId).getOrThrow()
            val txnTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

            ModelInput(
                txn_id = UUID.randomUUID().toString(),
                AMOUNT = amount,
                amount_sum_1h = featureStats.amountSum1h,
                TXN_TIMESTAMP = txnTimestamp,
                PAYER_VPA = currentUser.bankVPA,
                BENEFICIARY_VPA = receiver.bankVPA,
                PAYER_IFSC = currentUser.bankIFSC,
                BENEFICIARY_IFSC = receiver.bankIFSC,
                INITIATION_MODE = initiationMode,
                TRANSACTION_TYPE = transactionType,
                device_user_count = featureStats.deviceUserCount,
                txn_count_1h = featureStats.txnCount1h,
                failed_txn_count_24h = featureStats.failedTxnCount24h,
                consecutive_failures = featureStats.consecutiveFailures,
            )
        }
    }
}
