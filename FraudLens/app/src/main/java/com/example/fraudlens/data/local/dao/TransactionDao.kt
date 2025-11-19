//package com.example.fraudlens.data.local.dao
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import androidx.room.Transaction
//
//
//import com.example.fraudlens.data.local.entities.Transactions
//import com.example.fraudlens.data.local.entities.TransactionAndIPLog
//import com.example.fraudlens.data.local.entities.TransactionAndLocationLog
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.StateFlow
//
//@Dao
//interface TransactionDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTransaction(transaction: Transactions): Long
//
//    @Query("SELECT * FROM transactions WHERE payerUserId = :userId ORDER BY timestamp DESC")
//    fun getTransactionsForUser(userId: Long): Flow<List<Transactions>>
//
//    @Query("SELECT * FROM transactions WHERE receiverUserId = :userId ORDER BY timestamp DESC")
//    fun getTransactionsForReceivingUser(userId: Long): Flow<List<Transactions>>
//
//    @Query("SELECT * FROM transactions WHERE transactionId = :txnId")
//    suspend fun getTransactionById(txnId: Long): Transactions?
//
//    @Query("SELECT * FROM transactions WHERE receiverVpa = :vpa")
//    suspend fun getTransactionByVpa(vpa: String): Transactions?
//
//    @Transaction
//    @Query("SELECT * FROM transactions WHERE transactionId = :txnId")
//    suspend fun getTransactionWithIPLog(txnId: Long): TransactionAndIPLog
//
//    @Transaction
//    @Query("SELECT * FROM transactions WHERE transactionId = :txnId")
//    suspend fun getTransactionWithLocationLog(txnId: Long): TransactionAndLocationLog
//
//    @Delete
//    suspend fun deleteTransaction(transaction: Transactions)
//}
