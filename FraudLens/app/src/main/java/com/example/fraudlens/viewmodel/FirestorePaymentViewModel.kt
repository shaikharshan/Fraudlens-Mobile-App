package com.example.fraudlens.viewmodel

import android.Manifest
import kotlin.math.*
import kotlinx.coroutines.async
import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.fraudlens.data.local.entities.FirestoreCollection
import com.example.fraudlens.data.local.entities.FirestoreUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.example.fraudlens.data.local.entities.DEVIATION
import com.example.fraudlens.data.local.entities.FirestoreDeviceInfo
import com.example.fraudlens.data.local.entities.FirestoreIPLog
import com.example.fraudlens.data.local.entities.FirestoreLocationLog
import com.example.fraudlens.data.local.entities.FirestoreTransactions
import com.example.fraudlens.retrofit.AbuseRiskResult
import com.example.fraudlens.retrofit.ModelInput
//import com.example.fraudlens.utils.getDeviceInfo
import com.example.fraudlens.utils.getDeviceInfoFirestore
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class FirestorePaymentViewModel @Inject constructor(
    private val repository: FirebaseFirestore
) : ViewModel() {

    //set biometric flag
    fun setBiometricFlag(flag: Boolean){
        repository.collection(FirestoreCollection.USER.value)
            .document(loggedUser.value?.userId ?: "")
            .update("biometricEnabled",flag)
            .addOnSuccessListener { Log.d("check", "DocumentSnapshot successfully updated!") }
            .addOnFailureListener { e -> Log.w("check", "Error updating document", e) }
    }

    fun setLoggedUser(id: String){
        repository.collection(FirestoreCollection.USER.value)
            .document(id)
            .get()
            .addOnSuccessListener {
                _loggedUser.value = it.toObject<FirestoreUser>()
            }
            .addOnFailureListener {
                Log.e("check", "Failed to get user: ${it.message}")
            }
    }

    //get all users
    private var _userList = MutableStateFlow<List<FirestoreUser>>(emptyList())
    var userList = _userList.asStateFlow()
    fun getAllUsers() {
        repository.collection(FirestoreCollection.USER.value)
            .addSnapshotListener { value, err ->
                if (err != null) {
                    return@addSnapshotListener
                }
                if (value != null) {
                    _userList.value = value.toObjects<FirestoreUser>()
                }


            }
    }

    private var _transactions = MutableStateFlow<List<FirestoreTransactions>>(emptyList())
    var transactions = _transactions.asStateFlow()
    fun loadTransactions(userId: String) {
        val allTxns = mutableListOf<FirestoreTransactions>()

        repository.collection(FirestoreCollection.TRANSACTIONS.value)
            .whereEqualTo("payerUserId", userId)
            .get()
            .addOnSuccessListener { sentSnapshot ->
                allTxns.addAll(sentSnapshot.toObjects<FirestoreTransactions>())

                repository.collection(FirestoreCollection.TRANSACTIONS.value)
                    .whereEqualTo("receiverUserId", userId)
                    .get()
                    .addOnSuccessListener { recvSnapshot ->
                        allTxns.addAll(recvSnapshot.toObjects<FirestoreTransactions>())
                        _transactions.value = allTxns.sortedByDescending { it.timestamp }
                    }
            }
    }


    private var _loggedUser = MutableStateFlow<FirestoreUser?>(null)
    var loggedUser = _loggedUser.asStateFlow()

    private var _currentDevice = MutableStateFlow<FirestoreDeviceInfo?>(null)
    var currentDevice = _currentDevice.asStateFlow()

    private var tempUserId = MutableStateFlow("")

    fun checkLogin(
        context: Context,
        email: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        repository.collection(FirestoreCollection.USER.value)
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result ->
                val user = result.documents.firstOrNull()?.toObject<FirestoreUser>()
                if (user != null && user.password == password) {
                    _loggedUser.value = user

                    val currentDeviceId = Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ) ?: UUID.randomUUID().toString()

                    repository.collection(FirestoreCollection.USER.value)
                        .document(user.userId)
                        .collection(FirestoreCollection.DEVICE.value)
                        .get()
                        .addOnSuccessListener { devices ->
                            val matchedDevice = devices.toObjects(FirestoreDeviceInfo::class.java)
                                .find { it.deviceId == currentDeviceId }

                            if (matchedDevice != null) {
                                _currentDevice.value = matchedDevice
                            } else {
                                val newDevice = getDeviceInfoFirestore(context, user.userId)
                                repository.collection(FirestoreCollection.USER.value)
                                    .document(user.userId)
                                    .collection(FirestoreCollection.DEVICE.value)
                                    .add(newDevice)
                                _currentDevice.value = newDevice
                            }
                        }

                    onResult(true, "Login successful.")
                } else {
                    onResult(false, "Invalid credentials. Please try again or Sign Up.")
                }
            }
            .addOnFailureListener {
                onResult(false, "Something went wrong. Please try again.")
                Log.e("FirestoreLogin", it.message ?: "")
            }
    }

    fun signUpUser(
        context: Context,
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String, String) -> Unit
    ) {
        val newUserRef = repository.collection(FirestoreCollection.USER.value).document()
        val newUser = FirestoreUser(
            userId = newUserRef.id,
            username = name,
            email = email,
            password = password
        )

        newUserRef.set(newUser)
            .addOnSuccessListener {
                tempUserId.value = newUserRef.id
                _loggedUser.value = newUser

                val currentDeviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    ?: UUID.randomUUID().toString()

                val newDevice = getDeviceInfoFirestore(context, newUserRef.id)
                repository.collection(FirestoreCollection.USER.value)
                    .document(newUserRef.id)
                    .collection("devices")
                    .add(newDevice)

                _currentDevice.value = newDevice

                onResult(true, "User created successfully", newUserRef.id)
            }
            .addOnFailureListener {
                onResult(false, "Failed to create user", "")
                Log.e("FirestoreSignup", it.message ?: "")
            }
    }

    fun completeAccountSetup(
        vpa: String,
        ifsc: String,
        balance: Double,
        phone: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val userId = tempUserId.value
        val userRef = repository.collection(FirestoreCollection.USER.value).document(userId)

        viewModelScope.launch {
            userRef.get()
                .addOnSuccessListener { document ->
                    val existingUser = document.toObject<FirestoreUser>()
                    if (existingUser != null) {
                        val updatedUser = existingUser.copy(
                            bankVPA = vpa,
                            bankIFSC = ifsc,
                            balance = balance,
                            phone = phone
                        )
                        _loggedUser.value = updatedUser
                        userRef.set(updatedUser)
                        onResult(true, "Account creation successful. Proceeding to the app...")
                    } else {
                        onResult(false, "User not found. Please sign up correctly.")
                    }
                }
                .addOnFailureListener {
                    onResult(false, "Error updating user info.")
                    Log.e("FirestoreSetup", it.message ?: "")
                }
        }
    }

    //location and IP
    var _transactionIP = MutableStateFlow<String?>(null)
    var _transactionLocation = MutableStateFlow<Pair<Double, Double>?>(null)

    suspend fun fetchPublicIPs() = withContext(Dispatchers.IO) {
        try {
            URL("https://icanhazip.com/").openConnection().run {
                inputStream.bufferedReader().readText().trim()
            }
        } catch (e: Exception) {
            Log.d("custom_exception","$e")
            null }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @SuppressLint("MissingPermission")
    suspend fun fetchLocation(context: Context): Location? {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED) return null
        if(!isLocationEnabled(context)) return null

        return suspendCancellableCoroutine { cont ->
            LocationServices.getFusedLocationProviderClient(context).lastLocation
                .addOnSuccessListener { cont.resume(it, null) }
                .addOnFailureListener { cont.resume(null, null) }
        }
    }

    fun  isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    fun updateUserBalance(amount: Double,receiverId: String) {
        val payer = loggedUser.value ?: return

        val payerId = payer.userId
        val payerNewBalance = payer.balance - amount

        val receiverRef = repository.collection(FirestoreCollection.USER.value)
            .document(receiverId)

        val payerRef = repository.collection(FirestoreCollection.USER.value)
            .document(payerId)

        // Update receiver's balance (read first)
        receiverRef.get()
            .addOnSuccessListener { document ->
                val receiverBalance = document.getDouble("balance") ?: 0.0
                val updatedReceiverBalance = receiverBalance + amount

                // Update both balances
                receiverRef.update("balance", updatedReceiverBalance)
                    .addOnSuccessListener {
                        payerRef.update("balance", payerNewBalance)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Transfer successful")
                                // Update loggedUser balance locally for UI
                                _loggedUser.value = payer.copy(balance = payerNewBalance)
                            }
                            .addOnFailureListener {
                                Log.w("Firestore", "Failed to update payer balance", it)
                            }
                    }
                    .addOnFailureListener {
                        Log.w("Firestore", "Failed to update receiver balance", it)
                    }
            }
            .addOnFailureListener {
                Log.w("Firestore", "Failed to fetch receiver data", it)
            }

    }

    fun addTransaction(
        payerUserId: String,
        payerVpa: String,
        payerIFSC: String,
        payerDeviceId: String,
        receiverVpa: String,
        receiverIfsc: String,
        amount: Double,
        fraudScore: Float,
        locationRiskScore: Float,
        modelDecision: Boolean,
        status:String,
        ipRiskResult: AbuseRiskResult,
        location: Pair<Double,Double>,
        deviation: Double,
    ) {
        repository.collection(FirestoreCollection.USER.value)
            .whereEqualTo("bankVPA", receiverVpa)
            .get()
            .addOnSuccessListener { snapshot ->
                val receiver = snapshot.documents.firstOrNull()?.toObject<FirestoreUser>()
                if (receiver != null) {
                    val locationLog = FirestoreLocationLog(
                        latitude = location.first,
                        longitude = location.second,
                        deviationFromLast = deviation,
                        isSuspicious = deviation>= DEVIATION
                    )
                    val ipLog = FirestoreIPLog(
                        ipAddress = ipRiskResult.ip,
                        riskScore = ipRiskResult.abuseConfidenceScore.toFloat(),
                        isBlocked = ipRiskResult.isRisky,
                        country = ipRiskResult.country,
                        isp = ipRiskResult.isp)

                    repository.collection(FirestoreCollection.LOCATION.value)
                        .add(locationLog)
                        .addOnSuccessListener {locationDoc->

                            repository.collection(FirestoreCollection.IP.value)
                                .add(ipLog)
                                .addOnSuccessListener { ipDoc->
                                    Log.d("Firestore", "ip added")
                                    val transaction = FirestoreTransactions(
                                        payerUserId = payerUserId,
                                        payerDeviceId = payerDeviceId,
                                        payerVpa= payerVpa,
                                        payerIFSC= payerIFSC,
                                        receiverUserId = receiver.userId,
                                        receiverVpa = receiverVpa,
                                        receiverIfsc = receiverIfsc,
                                        amount = amount,
                                        status = status,
                                        fraudScore = fraudScore,
                                        ipRiskScore = ipRiskResult.abuseConfidenceScore.toFloat(),
                                        locationRiskScore = locationRiskScore,
                                        modelDecision = modelDecision,
                                        ipLogId = ipDoc.id,
                                        locationLogId = locationDoc.id
                                    )
                                    repository.collection(FirestoreCollection.TRANSACTIONS.value)
                                        .add(transaction)
                                        .addOnSuccessListener {transactionDoc->
                                            Log.d("Firestore", "Transaction added")
                                        }
                                        .addOnFailureListener {
                                            Log.e("Firestore", "Failed to add transaction: ${it.message}")
                                        }


                                }
                                .addOnFailureListener {
                                    Log.e("Firestore", "Failed to add ip: ${it.message}")
                                }

                            Log.d("Firestore", "Location added")
                        }
                        .addOnFailureListener {
                            Log.e("Firestore", "Failed to add location: ${it.message}")
                        }
                }
            }
    }



    private var _searchUsers = MutableStateFlow<List<FirestoreUser>>(emptyList())
    var searchUsers = _searchUsers.asStateFlow()
    fun getUsersByName(search: String) {
        repository.collection(FirestoreCollection.USER.value)
            .orderBy("bankVPA")
            .startAt(search)
            .endAt(search + "\uf8ff")
            .get()
            .addOnSuccessListener { snapshot ->
                _searchUsers.value = snapshot.toObjects<FirestoreUser>()
            }
            .addOnFailureListener {
                Log.d("searchVpa", it.toString())
            }
    }



    fun addUser(user: FirestoreUser) {
    repository.collection(FirestoreCollection.USER.value)
        .add(user)
        .addOnSuccessListener {
            Log.d("Firestore", "User added!")
        }
        .addOnFailureListener {
            Log.e("Firestore", "Failed to add user: ${it.message}")
        }
    }

    fun getSentTransactions(userId: String, onResult: (List<FirestoreTransactions>) -> Unit) {
        repository.collection(FirestoreCollection.TRANSACTIONS.value)
            .whereEqualTo("payerUserId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val transactions = snapshot.toObjects<FirestoreTransactions>()
                onResult(transactions)
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch sent transactions: ${it.message}")
                onResult(emptyList())
            }
    }

    fun getReceivedTransactions(userId: String, onResult: (List<FirestoreTransactions>) -> Unit) {
        repository.collection(FirestoreCollection.TRANSACTIONS.value)
            .whereEqualTo("receiverUserId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val transactions = snapshot.toObjects<FirestoreTransactions>()
                onResult(transactions)
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch received transactions: ${it.message}")
                onResult(emptyList())
            }
    }

    //updateUser
    fun updateUser(user:  Map<String, String>){
        repository.collection(FirestoreCollection.USER.value)
            .document(loggedUser.value?.userId ?: "NA")
            .update(user)
            .addOnSuccessListener { Log.d("check", "DocumentSnapshot successfully updated!") }
            .addOnFailureListener { e -> Log.w("check", "Error updating document", e) }
    }



    private var _deviationFromLast = MutableStateFlow<Double>(0.0)
    var deviationFromLast = _deviationFromLast.asStateFlow()
    var lastLocation: FirestoreLocationLog? = null
    var lastCoord:Pair<Double, Double>? = null

    fun checkLocationRisk() {
        val userId = loggedUser.value?.userId ?: return

        repository.collection(FirestoreCollection.USER.value)
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                repository.collection(FirestoreCollection.TRANSACTIONS.value)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { trDoc ->
                        val lastTransaction = trDoc.toObjects<FirestoreTransactions>().firstOrNull()
                        val locationId = lastTransaction?.locationLogId

                        if (locationId.isNullOrEmpty()) {
                            Log.w("check", "No valid locationLogId")
                            return@addOnSuccessListener
                        }

                        repository.collection(FirestoreCollection.LOCATION.value)
                            .document(locationId)
                            .get()
                            .addOnSuccessListener { locDoc ->
                                lastLocation = locDoc.toObject<FirestoreLocationLog>()
                                if (lastLocation != null) {
                                    lastCoord = Pair(lastLocation?.latitude!!, lastLocation?.longitude!!)
                                    _deviationFromLast.value = calculateDistance(lastCoord!!, _transactionLocation.value!!)
//                                    _deviationFromLast.value = 101.0
                                } else {
                                    Log.w("check", "Location document not found")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("check", "Error finding location", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.w("check", "Error finding last transaction", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.w("check", "Error finding current user", e)
            }
    }




    // Haversine formula to calculate distance between 2 lat/lon points
    private fun calculateDistance(
        location1: Pair<Double, Double>,
        location2: Pair<Double, Double>
    ): Double {
        val earthRadiusKm = 6371.0

        val (lat1, lon1) = location1
        val (lat2, lon2) = location2

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = earthRadiusKm * c

        return (distanceKm)
    }




    // AI model functions

suspend fun prepareModelInput(
        receiver: FirestoreUser,
        amount: Double
    ): ModelInput? {
        val currentUser = loggedUser.value
        val payerVpa = currentUser?.bankVPA
        val payerIfsc = currentUser?.bankIFSC
        val txnTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val txnId = UUID.randomUUID().toString()
        try {

            val deviceUserCountSnapshot = repository
                    .collection(FirestoreCollection.USER.value)
                .document(currentUser?.userId ?: "")
                .collection(FirestoreCollection.DEVICE.value)
                .get()
                .await()

            val deviceUserCount = deviceUserCountSnapshot.size()


            // Get transactions in past hour by current user
            val oneHourAgo = Timestamp(Date(System.currentTimeMillis() - (3600 * 1000)))


            val txnCountSnapshot = repository
                .collection(FirestoreCollection.TRANSACTIONS.value)
                .whereEqualTo("payerUserId", currentUser?.userId)
                .whereGreaterThan("timestamp", oneHourAgo)
                .get()
                .await()

            val txnCount1h = txnCountSnapshot.size()

            val input = ModelInput(
                txn_id = txnId,
                AMOUNT = amount,
                TXN_TIMESTAMP = txnTimestamp,
                PAYER_VPA = payerVpa!!,
                BENEFICIARY_VPA = receiver.bankVPA,
                PAYER_IFSC = payerIfsc!!,
                BENEFICIARY_IFSC = receiver.bankIFSC,
                device_user_count = deviceUserCount,
                txn_count_1h = txnCount1h
            )
            return input
        } catch (e: Exception) {
            Log.e("ModelInputError", "Failed to prepare input: ${e.message}")
            return null
        }
}

}   
