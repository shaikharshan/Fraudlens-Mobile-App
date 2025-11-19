//package com.example.fraudlens.data.local.database
//
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import com.example.fraudlens.data.local.dao.DeviceInfoDao
//import com.example.fraudlens.data.local.dao.IPLogDao
//import com.example.fraudlens.data.local.dao.LocationLogDao
//import com.example.fraudlens.data.local.dao.TransactionDao
//import com.example.fraudlens.data.local.dao.UserDao
//import com.example.fraudlens.data.local.entities.DeviceInfo
//import com.example.fraudlens.data.local.entities.IPLog
//import com.example.fraudlens.data.local.entities.LocationLog
//import com.example.fraudlens.data.local.entities.Transactions
//import com.example.fraudlens.data.local.entities.User
//import android.content.Context
//
//@Database(
//    entities = [User::class, DeviceInfo::class, Transactions::class, IPLog::class, LocationLog::class],
//    version = 2,
//    exportSchema = false
//)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun userDao(): UserDao
//    abstract fun deviceInfoDao(): DeviceInfoDao
//    abstract fun transactionDao(): TransactionDao
//    abstract fun ipLogDao(): IPLogDao
//    abstract fun locationLogDao(): LocationLogDao
//
//
//    companion object {
//        @Volatile
//        private var Instance: AppDatabase? = null
//
//        fun getDatabase(context: Context): AppDatabase {
//            // if the Instance is not null, return it, otherwise create a new database instance.
//            return Instance ?: synchronized(this) {
//                Room.databaseBuilder(context, AppDatabase::class.java, "item_database")
//                    /**
//                     * Setting this option in your app's database builder means that Room
//                     * permanently deletes all data from the tables in your database when it
//                     * attempts to perform a migration with no defined migration path.
//                     */
//                    .fallbackToDestructiveMigration()
//                    .build()
//                    .also { Instance = it }
//            }
//        }
//    }
//}
