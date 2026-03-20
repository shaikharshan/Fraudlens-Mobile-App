package com.fraudlens.sdk.firebase

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Thin Firestore helpers aligned with the FraudLens demo schema.
 * The host app must apply `google-services` and initialize Firebase before use.
 */
class FraudLensFirestoreRepository(
    private val firestore: FirebaseFirestore,
) {

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
}
