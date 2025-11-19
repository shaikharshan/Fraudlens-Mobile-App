//package com.example.fraudlens.data.repo
//
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
//import kotlinx.coroutines.flow.Flow
//
//class PaymentRepository(
//    private val userDao: UserDao,
//    private val deviceInfoDao: DeviceInfoDao,
//    private val transactionDao: TransactionDao,
//    private val ipLogDao: IPLogDao,
//    private val locationLogDao: LocationLogDao
//) {
//
//    // --- USER ---
//    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
//
//    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
//
//    suspend fun updateUser(user:User): Unit = userDao.updateUser(user)
//    suspend fun updateUserBalance(balance: Double, userId: Long): Unit = userDao.updateUserBalance(balance, userId)
//    suspend fun getUserWithDevices(userId: Int) = userDao.getUserWithDevices(userId)
//    suspend fun getUserWithTransactions(userId: Int) = userDao.getUserWithTransactions(userId)
//    suspend fun getUserByID(userId:Long): User? = userDao.getUserById(userId)
//    suspend fun getUserByEmail(email: String) : User? = userDao.getUserByEmail(email)
//    suspend fun getUserByVpa(vpa: String) : User? = userDao.getUserByVpa(vpa)
//    fun getUserByName(vpa: String) : Flow<List<User>> = userDao.getUserByName(vpa)
//
//    // --- DEVICE ---
//    suspend fun insertDevice(device: DeviceInfo): Long = deviceInfoDao.insertDevice(device)
//    suspend fun getDevicesByUser(userId: Long) = deviceInfoDao.getDevicesByUser(userId)
//    suspend fun countDevices(userId: Long) = deviceInfoDao.countDevicesForUser(userId)
//
//
//    // --- TRANSACTION ---
//    suspend fun insertTransaction(txn: Transactions): Long = transactionDao.insertTransaction(txn)
//    fun getTransactions(userId: Long) = transactionDao.getTransactionsForUser(userId)
//    fun getTransactionsForReceivingUser(userId: Long) = transactionDao.getTransactionsForReceivingUser(userId)
//    suspend fun getTransactionByVpa(vpa: String) = transactionDao.getTransactionByVpa(vpa)
//
//
//    suspend fun getTransaction(txnId: Long) = transactionDao.getTransactionById(txnId)
//
//    // --- Logs ---
//    suspend fun insertIPLog(log: IPLog): Long = ipLogDao.insertIPLog(log)
//    suspend fun insertLocationLog(log: LocationLog): Long = locationLogDao.insertLocationLog(log)
//
//    suspend fun getIPLog(txnId: Long) = ipLogDao.getIPLogForTransaction(txnId)
//    suspend fun getLocationLog(txnId: Long) = locationLogDao.getLocationLogForTransaction(txnId)
//
//    suspend fun getTransactionWithIPLog(txnId: Long) = transactionDao.getTransactionWithIPLog(txnId)
//    suspend fun getTransactionWithLocationLog(txnId: Long) = transactionDao.getTransactionWithLocationLog(txnId)
//}
