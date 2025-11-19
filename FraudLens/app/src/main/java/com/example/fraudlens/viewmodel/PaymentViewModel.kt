//package com.example.fraudlens.viewmodel
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.Context
//import android.provider.Settings
//import android.util.Log
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//
//import com.example.fraudlens.data.local.entities.DeviceInfo
//import com.example.fraudlens.data.local.entities.IPLog
//import com.example.fraudlens.data.local.entities.LocationLog
//import com.example.fraudlens.data.local.entities.Transactions
//import com.example.fraudlens.data.local.entities.User
//import com.example.fraudlens.data.repo.PaymentRepository
//import com.example.fraudlens.utils.getDeviceInfo
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import java.util.UUID
//import javax.inject.Inject
//
//import com.google.android.gms.location.LocationServices
//
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.suspendCancellableCoroutine
//import kotlinx.coroutines.withContext
//import android.content.pm.PackageManager
//import android.location.Location
//import android.location.LocationManager
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.viewmodel.compose.viewModel
//
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.firstOrNull
//
//import java.net.URL
//
//@ExperimentalCoroutinesApi
//@HiltViewModel
//class PaymentViewModel @Inject constructor(
//    private val repository: PaymentRepository
//) : ViewModel() {
//
//    private var loggedUser = MutableStateFlow<User?>(null)
//    var currentUser = loggedUser.asStateFlow()
//     var _currentDevice = MutableStateFlow<DeviceInfo?>(null)
//    private var tempUserId = MutableStateFlow<Long>(0)
//
//    var currentRecipient = MutableStateFlow<User?>(null)
//    var _users = MutableStateFlow<List<User>>(emptyList())
//    val users: StateFlow<List<User>> = _users.asStateFlow()
//
//    var searchUsers = MutableStateFlow<List<User>>(emptyList())
//
//    var _transactions = MutableStateFlow<List<Transactions>>(emptyList())
//    val transactions: StateFlow<List<Transactions>> = _transactions.asStateFlow()
//
//      var _transactionIP = MutableStateFlow<String?>(null)
//
//      var _transactionLocation = MutableStateFlow<Pair<Double, Double>?>(null)
//
//    suspend fun fetchPublicIPs() = withContext(Dispatchers.IO) {
//        try {
//            URL("https://icanhazip.com/").openConnection().run {
//                inputStream.bufferedReader().readText().trim()
//            }
//        } catch (e: Exception) {
//            Log.d("custom_exception","$e")
//            null }
//    }
//
//    @SuppressLint("MissingPermission")
//    suspend fun fetchLocation(context: Context): Location? {
//        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
//        if (fine != PackageManager.PERMISSION_GRANTED) return null
//        if(!isLocationEnabled(context)) return null
//
//        return suspendCancellableCoroutine { cont ->
//            LocationServices.getFusedLocationProviderClient(context).lastLocation
//                .addOnSuccessListener { cont.resume(it, null) }
//                .addOnFailureListener { cont.resume(null, null) }
//        }
//    }
//
//    fun isLocationEnabled(context: Context): Boolean {
//        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
//                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
//    }
//
//    fun loadAllUsers() {
//        viewModelScope.launch {
//            repository.getAllUsers()
//            .catch { exception -> Log.e("TAG", exception.localizedMessage ?: "Error") }
//            .collect {
//                _users.value = it
//            }
//        }
//    }
//
//    fun loadTransactions(userId: Long) {
//        viewModelScope.launch {
//            val sentTransactions = repository.getTransactions(userId)
//                .catch { exception -> Log.e("TAG", exception.localizedMessage ?: "Error") }
//                .firstOrNull() ?: emptyList()
//
//            val receivedTransactions = repository.getTransactionsForReceivingUser(userId)
//                .catch { exception -> Log.e("TAG", exception.localizedMessage ?: "Error") }
//                .firstOrNull() ?: emptyList()
//
//            // Combine and sort by timestamp
//            val allTxns = (sentTransactions + receivedTransactions)
//                .sortedByDescending { it.timestamp }
//
//            _transactions.value = allTxns
//        }
//    }
//
//    fun getUsersByName(vpa:String){
//        viewModelScope.launch {
//            repository.getUserByName(vpa)
//                .catch { exception -> Log.e("TAG", exception.localizedMessage ?: "Error") }
//                .   collect {
//                    searchUsers.value = it
//                }
//
//        }
//    }
//
//
//    fun addTransaction(
//        payerUserId: Long,
//        payerDeviceId: String,
//        receiverVpa: String,
//        receiverIfsc: String,
//        amount: Double,
//        fraudScore: Float,
//        ipRiskScore: Float,
//        locationRiskScore: Float,
//        modelDecision: Boolean
//    ) {
//        viewModelScope.launch {
//            val temp = repository.getUserByVpa(receiverVpa)
//        val txn = Transactions(
//            payerUserId = payerUserId,
//            payerDeviceId = payerDeviceId,
//            receiverUserId = temp?.userId?.toLong() ?: 0L,
//            receiverVpa = receiverVpa,
//            receiverIfsc = receiverIfsc,
//            amount = amount,
//            status = if (!modelDecision) "APPROVED" else "BLOCKED",
//            fraudScore = fraudScore,
//            ipRiskScore = ipRiskScore,
//            locationRiskScore = locationRiskScore,
//            modelDecision = modelDecision
//        )
//
//            repository.insertTransaction(txn)
//        }
//    }
//
//    fun addUser(user: User) {
//        viewModelScope.launch {
//            repository.insertUser(user)
//        }
//    }
//
//    fun updateUserBalance(balance: Double, userId: Long){
//        viewModelScope.launch {
//            repository.updateUserBalance(balance,userId)
//            val updatedUser = repository.getUserByID(userId)
//            if (updatedUser != null) {
//                loggedUser.value = updatedUser
//            }
//        }
//    }
//
//    fun checkLogin(
//        context: Context,
//        email: String,
//        password: String,
//        onResult: (Boolean, String) -> Unit
//    ) {
//        viewModelScope.launch {
//            try {
//                val checkUser = repository.getUserByEmail(email)
//                if (checkUser != null && checkUser.password == password) {
//                    loggedUser.value = checkUser.copy()
//
//                    val existingDevices = repository.getDevicesByUser(checkUser.userId.toLong())
//
//                    val currentDeviceId = Settings.Secure.getString(
//                        context.contentResolver,
//                        Settings.Secure.ANDROID_ID
//                    ) ?: UUID.randomUUID().toString() // fallback if null
//
//                    val matchedDevice = existingDevices.find { it.deviceId == currentDeviceId }
//
//                    if (matchedDevice != null) {
//                        _currentDevice.value = matchedDevice.copy()
//                    } else {
//                        val newDevice = getDeviceInfo(context, checkUser.userId.toLong())
//                        repository.insertDevice(newDevice)
//                        _currentDevice.value = newDevice
//                    }
//
//                    onResult(true, "Login successful.")
//                } else {
//                    onResult(false, "Invalid credentials. Please try again or Sign Up.")
//                }
//            } catch (e: Exception) {
//                Log.e("LoginError", "Login failed: ${e.message}", e)
//                onResult(false, "Something went wrong. Please try again.")
//            }
//        }
//    }
//
//    fun signUpUser(
//        context: Context,
//        name: String,
//        email: String,
//        password: String,
//        onResult: (Boolean, String,Long) -> Unit
//    ) {
//        viewModelScope.launch {
//            val checkUser = repository.getUserByEmail(email)
//            if (checkUser != null) {
//                onResult(false, "User already exists. Please log in.", -100)
//                return@launch
//            }
//
//            val newUser = User(username = name, email = email, password = password)
//            val userId = repository.insertUser(newUser)
//            tempUserId.value = userId
//
//            val existingDevices = repository.getDevicesByUser(userId)
//            val currentDeviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
//                ?: UUID.randomUUID().toString()
//
//            val foundDevice = existingDevices.find { it.deviceId == currentDeviceId }
//
//            if (foundDevice != null) {
//                _currentDevice.value = foundDevice.copy()
//            } else {
//                val newDevice = getDeviceInfo(context, userId)
//                repository.insertDevice(newDevice)
//                _currentDevice.value = newDevice
//            }
//
//            onResult(true, "User created successfully", userId)
//        }
//    }
//    fun completeAccountSetup(
//        vpa: String,
//        ifsc: String,
//        balance: Double,
//        phone:String,
//        onResult: (Boolean, String) -> Unit
//    ) {
//        viewModelScope.launch {
//            Log.d("Temp ID","${tempUserId.value.toInt()}")
//            val existingUser:User? = repository.getUserByID(tempUserId.value)
//            if (existingUser != null) {
//                val updatedUser = existingUser.copy(
//                    bankVPA = vpa,
//                    bankIFSC = ifsc,
//                    balance = balance,
//                    phone = phone
//                )
//                loggedUser.value = updatedUser.copy()
//                repository.updateUser(updatedUser)
//                onResult(true,"Account creation successful. Proceeding to the app..")
//            }
//            else{
//                onResult(false,"User not found. SignUp correctly to proceed")
//            }
//        }
//    }
//
//     fun getUserById(id:Long){
//         viewModelScope.launch {
//             currentRecipient.value = repository.getUserByID(id)
//         }
//    }
//
//
//    fun addDevice(device: DeviceInfo) {
//        viewModelScope.launch {
//            repository.insertDevice(device)
//        }
//    }
//
//    fun addIPLog(log: IPLog) {
//        viewModelScope.launch {
//            repository.insertIPLog(log)
//        }
//    }
//
//    fun addLocationLog(log: LocationLog) {
//        viewModelScope.launch {
//            repository.insertLocationLog(log)
//        }
//    }
//}
