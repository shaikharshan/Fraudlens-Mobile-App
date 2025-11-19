//package com.example.fraudlens.data.local.dao
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import com.example.fraudlens.data.local.entities.LocationLog
//import kotlinx.coroutines.flow.Flow
//
//
//
//@Dao
//interface LocationLogDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertLocationLog(log: LocationLog): Long
//
//    @Query("SELECT * FROM location_logs WHERE transactionId = :txnId")
//    suspend fun getLocationLogForTransaction(txnId: Long): LocationLog?
//
//    @Query("SELECT * FROM location_logs WHERE isSuspicious = 1")
//    fun getSuspiciousLocations(): Flow<List<LocationLog>>
//
//    @Delete
//    suspend fun deleteLocationLog(log: LocationLog)
//}
