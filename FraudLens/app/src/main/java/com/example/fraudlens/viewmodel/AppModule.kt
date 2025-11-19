package com.example.fraudlens.viewmodel

//import android.app.Application
//import android.content.Context
//import androidx.room.Room
//import com.example.fraudlens.data.local.dao.DeviceInfoDao
//import com.example.fraudlens.data.local.dao.IPLogDao
//import com.example.fraudlens.data.local.dao.LocationLogDao
//import com.example.fraudlens.data.local.dao.TransactionDao
//import com.example.fraudlens.data.local.dao.UserDao
//import com.example.fraudlens.data.local.database.AppDatabase
//import com.example.fraudlens.data.repo.PaymentRepository
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.HiltAndroidApp
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//
//@Module
//@HiltAndroidApp
//@InstallIn(SingletonComponent::class)
//class AppModule : Application(){
//
//    @Provides
//    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
//        AppDatabase.getDatabase(context = context)
//
//    @Provides
//    fun provideUserDao(db: AppDatabase) = db.userDao()
//
//    @Provides
//    fun provideDeviceDao(db: AppDatabase) = db.deviceInfoDao()
//
////    @Provides
//    fun provideTransactionDao(db: AppDatabase) = db.transactionDao()
//
//    @Provides
//    fun provideIPLogDao(db: AppDatabase) = db.ipLogDao()
//
//    @Provides
//    fun provideLocationLogDao(db: AppDatabase) = db.locationLogDao()
//
//    @Provides
//    fun provideRepository(
//        userDao: UserDao,
//        deviceInfoDao: DeviceInfoDao,
//        transactionDao: TransactionDao,
//        ipLogDao: IPLogDao,
//        locationLogDao: LocationLogDao
//    ): PaymentRepository = PaymentRepository(
//        userDao, deviceInfoDao, transactionDao, ipLogDao, locationLogDao
//    )
//}
