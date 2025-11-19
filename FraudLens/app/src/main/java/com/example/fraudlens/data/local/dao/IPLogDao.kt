//package com.example.fraudlens.data.local.dao
//
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import com.example.fraudlens.data.local.entities.IPLog
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface IPLogDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertIPLog(ipLog: IPLog): Long
//
//    @Query("SELECT * FROM ip_log WHERE transactionId = :txnId")
//    suspend fun getIPLogForTransaction(txnId: Long): IPLog?
//
//    @Query("SELECT * FROM ip_log WHERE isBlocked = 1")
//    fun getBlockedIPs(): Flow<List<IPLog>>
//
//    @Delete
//    suspend fun deleteIPLog(ipLog: IPLog)
//}
