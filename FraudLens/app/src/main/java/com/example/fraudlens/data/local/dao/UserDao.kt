//package com.example.fraudlens.data.local.dao
//
//import androidx.room.Dao
//import androidx.room.Delete
//import androidx.room.Insert
//import androidx.room.OnConflictStrategy
//import androidx.room.Query
//import androidx.room.Transaction
//import androidx.room.Update
//import com.example.fraudlens.data.local.entities.User
//import com.example.fraudlens.data.local.entities.UserWithDevices
//import com.example.fraudlens.data.local.entities.UserWithTransactions
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.StateFlow
//
//@Dao
//interface UserDao {
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertUser(user: User): Long
//
//    @Query("SELECT * FROM user WHERE userId = :id")
//    suspend fun getUserById(id: Long): User?
//
//    @Query("SELECT * FROM user WHERE bankVPA = :vpa")
//    suspend fun getUserByVpa(vpa: String): User?
//
//    @Query("SELECT * FROM user WHERE email= :email")
//    suspend fun getUserByEmail(email: String):User?
//
//    @Query("SELECT * FROM user WHERE bankVPA LIKE LOWER(:search)")
//    fun getUserByName(search: String): Flow<List<User>>
//
//    @Update
//    suspend fun updateUser(user: User)
//
//    @Query("UPDATE user SET balance=:balance where userId=:userId")
//    suspend fun updateUserBalance(balance: Double, userId: Long)
//
//    @Transaction
//    @Query("SELECT * FROM user WHERE userId = :userId")
//    suspend fun getUserWithDevices(userId: Int): UserWithDevices
//
//    @Transaction
//    @Query("SELECT * FROM user WHERE userId = :userId")
//    suspend fun getUserWithTransactions(userId: Int): UserWithTransactions
//
//    @Query("SELECT * FROM user")
//    fun getAllUsers(): Flow<List<User>>
//
//
//    @Delete
//    suspend fun deleteUser(user: User)
//}
