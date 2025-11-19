//package com.example.fraudlens.data.local.dao
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import com.example.fraudlens.data.local.entities.DeviceInfo
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface DeviceInfoDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertDevice(device: DeviceInfo): Long
//
//    @Query("SELECT * FROM device_info WHERE userId = :userId")
//    suspend fun getDevicesByUser(userId: Long): List<DeviceInfo>
//
//    @Query("SELECT COUNT(*) FROM device_info WHERE userId = :userId")
//    suspend fun countDevicesForUser(userId: Long): Int
//
//    @Delete
//    suspend fun deleteDevice(device: DeviceInfo)
//}
