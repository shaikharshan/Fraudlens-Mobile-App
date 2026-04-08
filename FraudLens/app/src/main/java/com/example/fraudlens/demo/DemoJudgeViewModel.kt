package com.example.fraudlens.demo

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fraudlens.data.local.entities.DEVIATION
import com.example.fraudlens.data.local.entities.FirestoreCollection
import com.example.fraudlens.data.local.entities.FirestoreIPLog
import com.example.fraudlens.data.local.entities.FirestoreLocationLog
import com.example.fraudlens.data.local.entities.FirestoreTransactions
import com.example.fraudlens.data.local.entities.FirestoreUser
import com.example.fraudlens.data.local.entities.TransactionResponse
import com.example.fraudlens.retrofit.AbuseRiskResult
import com.example.fraudlens.retrofit.IPAbuseRiskHelper
import com.example.fraudlens.retrofit.ModelInput
import com.example.fraudlens.retrofit.ModelOutput
import com.example.fraudlens.retrofit.RazorpayOrderRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DemoJudgeViewModel(application: Application) : AndroidViewModel(application) {
    private val firestore = FirebaseFirestore.getInstance()
    private val retrofitVm = com.example.fraudlens.viewmodel.RetrofitViewModel()

    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    private var paymentResultSignal: CompletableDeferred<com.example.fraudlens.retrofit.RazorpayPaymentResult>? = null

    fun onConfigChange(newConfig: DemoConfig) {
        _uiState.value = _uiState.value.copy(config = newConfig)
    }

    fun attachPaymentSignal(signal: CompletableDeferred<com.example.fraudlens.retrofit.RazorpayPaymentResult>) {
        paymentResultSignal = signal
    }

    fun startDemo(launchRazorpayCheckout: suspend (orderId: String, amount: Double, payer: FirestoreUser, receiver: FirestoreUser) -> Unit) {
        if (_uiState.value.runnerStatus == DemoRunnerStatus.RUNNING) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                runnerStatus = DemoRunnerStatus.RUNNING,
                currentScenario = null,
                awaitingNext = false,
                latestError = null,
                results = emptyList(),
                logLines = emptyList()
            )
            try {
                val config = _uiState.value.config
                val payer = resolvePayer(config.payerUserId)
                    ?: error("Payer not found. Enter Firestore user id or payer bank VPA.")
                val receiver = getRecipient(config.recipientVpa, payer.userId)
                    ?: error("Recipient not found. Enter a valid recipient VPA.")

                runScenarioNormal(payer, receiver, config, launchRazorpayCheckout)
                pauseForJudge("Scenario 1 complete. Tap Next Scenario.")

                runScenarioRiskyIpAndLocation(payer, receiver, config)
                pauseForJudge("Scenario 2 complete. Tap Next Scenario.")

                runScenarioMlTriggeredFraud(payer, receiver, config)
                _uiState.value = _uiState.value.copy(
                    runnerStatus = DemoRunnerStatus.COMPLETED,
                    currentScenario = null,
                    awaitingNext = false
                )
                addLog("Demo finished.")
            } catch (t: Throwable) {
                _uiState.value = _uiState.value.copy(
                    runnerStatus = DemoRunnerStatus.ERROR,
                    latestError = t.message ?: "Unknown demo error",
                    awaitingNext = false
                )
                addLog("ERROR: ${t.message}")
            }
        }
    }

    fun continueAfterPause() {
        if (_uiState.value.runnerStatus != DemoRunnerStatus.PAUSED) return
        _uiState.value = _uiState.value.copy(runnerStatus = DemoRunnerStatus.RUNNING, awaitingNext = false)
    }

    private suspend fun pauseForJudge(message: String) {
        addLog(message)
        _uiState.value = _uiState.value.copy(runnerStatus = DemoRunnerStatus.PAUSED, awaitingNext = true)
        while (_uiState.value.runnerStatus == DemoRunnerStatus.PAUSED) {
            delay(300)
        }
    }

    private suspend fun runScenarioNormal(
        payer: FirestoreUser,
        receiver: FirestoreUser,
        config: DemoConfig,
        launchRazorpayCheckout: suspend (orderId: String, amount: Double, payer: FirestoreUser, receiver: FirestoreUser) -> Unit
    ) {
        _uiState.value = _uiState.value.copy(currentScenario = DemoScenarioType.NORMAL_PAYMENT)
        addLog("Running Scenario 1 (normal): fetching real IP and GPS.")
        val realIp = fetchPublicIp() ?: error("Unable to fetch public IP for normal scenario.")
        val realLocation = fetchCurrentLocation()
            ?: error("Unable to fetch current location for normal scenario. Enable location and permission.")
        val locationPair = realLocation.latitude to realLocation.longitude
        val ipRisk = checkIpRisk(realIp) ?: error("IP check failed in normal scenario.")
        val deviation = computeDeviationFromLastTransaction(payer.userId, locationPair)

        val modelOutput = runModelPrediction(
            payer = payer,
            receiver = receiver,
            amount = config.normalAmount,
            preferFraud = false
        )

        if (ipRisk.isRisky || deviation >= DEVIATION || (modelOutput?.is_fraud == true)) {
            addLog("Warning: normal scenario came out risky; still proceeding to Razorpay for demo continuity.")
        }

        val order = createRazorpayOrder(config.normalAmount)
        addLog("Razorpay order created: ${order.id}")
        launchRazorpayCheckout(order.id, config.normalAmount, payer, receiver)

        val paymentSignal = paymentResultSignal ?: error("Payment signal unavailable.")
        val result = paymentSignal.await()
        if (!result.isSuccess) {
            error("Normal scenario payment failed: ${result.errorMessage}")
        }
        addLog("Razorpay payment success: ${result.paymentId}")

        val txIdHint = writeTransaction(
            payer = payer,
            receiver = receiver,
            amount = config.normalAmount,
            ipRisk = ipRisk,
            location = locationPair,
            deviation = deviation,
            modelOutput = modelOutput ?: defaultModelOutput(),
            status = TransactionResponse.APPROVED.value,
            razorpayOrderId = order.id,
            razorpayPaymentId = result.paymentId
        )

        appendResult(
            DemoScenarioResult(
                scenario = DemoScenarioType.NORMAL_PAYMENT,
                statusText = "Normal flow completed with Razorpay",
                transactionStatus = TransactionResponse.APPROVED.value,
                transactionIdHint = txIdHint,
                ipRisk = ipRisk,
                modelOutput = modelOutput,
                locationDeviationKm = deviation
            )
        )
    }

    private suspend fun runScenarioRiskyIpAndLocation(
        payer: FirestoreUser,
        receiver: FirestoreUser,
        config: DemoConfig
    ) {
        _uiState.value = _uiState.value.copy(currentScenario = DemoScenarioType.RISKY_IP_AND_LOCATION)
        addLog("Running Scenario 2: override risky IP + far location.")

        val ipRisk = checkIpRisk(config.riskyIpForOverride)
            ?: error("Risky IP check failed. Provide a valid risky IP.")

        val forcedLocation = config.farLatitude to config.farLongitude
        val deviation = computeDeviationFromLastTransaction(payer.userId, forcedLocation)
        addLog("Scenario 2 deviation: ${"%.2f".format(deviation)} km")

        val modelOutput = runModelPrediction(
            payer = payer,
            receiver = receiver,
            amount = config.riskyAmount,
            preferFraud = true
        ) ?: defaultModelOutput()

        runAutoBlockCountdown()

        val txIdHint = writeTransaction(
            payer = payer,
            receiver = receiver,
            amount = config.riskyAmount,
            ipRisk = ipRisk,
            location = forcedLocation,
            deviation = deviation,
            modelOutput = modelOutput,
            status = TransactionResponse.BLOCKED.value
        )

        appendResult(
            DemoScenarioResult(
                scenario = DemoScenarioType.RISKY_IP_AND_LOCATION,
                statusText = "Risky IP + far-away location blocked after countdown",
                transactionStatus = TransactionResponse.BLOCKED.value,
                transactionIdHint = txIdHint,
                ipRisk = ipRisk,
                modelOutput = modelOutput,
                locationDeviationKm = deviation
            )
        )
    }

    private suspend fun runScenarioMlTriggeredFraud(
        payer: FirestoreUser,
        receiver: FirestoreUser,
        config: DemoConfig
    ) {
        _uiState.value = _uiState.value.copy(currentScenario = DemoScenarioType.ML_TRIGGERED_FRAUD)
        addLog("Running Scenario 3: ML-triggered fraud with deterministic high-risk model input.")

        val safeIp = fetchPublicIp() ?: config.riskyIpForOverride
        val ipRisk = checkIpRisk(safeIp)
            ?: error("IP check failed in ML scenario.")
        val realLocation = fetchCurrentLocation()
            ?: error("Unable to fetch current location for ML scenario.")
        val locPair = realLocation.latitude to realLocation.longitude
        val deviation = computeDeviationFromLastTransaction(payer.userId, locPair)

        val modelOutput = runModelPrediction(
            payer = payer,
            receiver = receiver,
            amount = config.mlAmount,
            preferFraud = true
        ) ?: error("ML prediction failed in scenario 3.")

        if (!modelOutput.is_fraud && modelOutput.fraud_probability < 0.5f) {
            error("ML scenario did not trigger fraud. Try larger mlAmount or different users.")
        }

        runAutoBlockCountdown()

        val txIdHint = writeTransaction(
            payer = payer,
            receiver = receiver,
            amount = config.mlAmount,
            ipRisk = ipRisk,
            location = locPair,
            deviation = deviation,
            modelOutput = modelOutput,
            status = TransactionResponse.BLOCKED.value
        )

        appendResult(
            DemoScenarioResult(
                scenario = DemoScenarioType.ML_TRIGGERED_FRAUD,
                statusText = "ML fraud signal triggered block after countdown",
                transactionStatus = TransactionResponse.BLOCKED.value,
                transactionIdHint = txIdHint,
                ipRisk = ipRisk,
                modelOutput = modelOutput,
                locationDeviationKm = deviation
            )
        )
    }

    private suspend fun runAutoBlockCountdown() {
        for (sec in 15 downTo 1) {
            _uiState.value = _uiState.value.copy(countdownSeconds = sec)
            delay(1000)
        }
        _uiState.value = _uiState.value.copy(countdownSeconds = 0)
    }

    private suspend fun createRazorpayOrder(amount: Double): com.example.fraudlens.retrofit.RazorpayOrderResponse {
        val request = RazorpayOrderRequest(
            amount = (amount * 100).toInt(),
            currency = "INR",
            receipt = UUID.randomUUID().toString(),
            notes = mapOf("demo_mode" to "judge_flow")
        )
        val response = DemoRazorpayClient.api.createOrder(request)
        if (!response.isSuccessful || response.body() == null) {
            error("Failed to create Razorpay order: ${response.code()} ${response.message()}")
        }
        return response.body()!!
    }

    private suspend fun runModelPrediction(
        payer: FirestoreUser,
        receiver: FirestoreUser,
        amount: Double,
        preferFraud: Boolean
    ): ModelOutput? {
        val input = if (preferFraud) {
            ModelInput(
                txn_id = UUID.randomUUID().toString(),
                AMOUNT = amount,
                amount_sum_1h = amount * 7,
                TXN_TIMESTAMP = Timestamp.now().toDate().toString(),
                PAYER_VPA = payer.bankVPA,
                BENEFICIARY_VPA = receiver.bankVPA,
                PAYER_IFSC = payer.bankIFSC,
                BENEFICIARY_IFSC = receiver.bankIFSC,
                INITIATION_MODE = "APP",
                TRANSACTION_TYPE = "P2P",
                device_user_count = 6,
                txn_count_1h = 12,
                failed_txn_count_24h = 6,
                consecutive_failures = 4
            )
        } else {
            ModelInput(
                txn_id = UUID.randomUUID().toString(),
                AMOUNT = amount,
                amount_sum_1h = amount,
                TXN_TIMESTAMP = Timestamp.now().toDate().toString(),
                PAYER_VPA = payer.bankVPA,
                BENEFICIARY_VPA = receiver.bankVPA,
                PAYER_IFSC = payer.bankIFSC,
                BENEFICIARY_IFSC = receiver.bankIFSC,
                INITIATION_MODE = "APP",
                TRANSACTION_TYPE = "P2P",
                device_user_count = 1,
                txn_count_1h = 1,
                failed_txn_count_24h = 0,
                consecutive_failures = 0
            )
        }
        return retrofitVm.predictFraud(input)
    }

    private suspend fun writeTransaction(
        payer: FirestoreUser,
        receiver: FirestoreUser,
        amount: Double,
        ipRisk: AbuseRiskResult,
        location: Pair<Double, Double>,
        deviation: Double,
        modelOutput: ModelOutput,
        status: String,
        razorpayOrderId: String? = null,
        razorpayPaymentId: String? = null
    ): String {
        val locationLog = FirestoreLocationLog(
            latitude = location.first,
            longitude = location.second,
            deviationFromLast = deviation,
            isSuspicious = deviation >= DEVIATION
        )
        val locationRef = firestore.collection(FirestoreCollection.LOCATION.value).add(locationLog).await()

        val ipLog = FirestoreIPLog(
            ipAddress = ipRisk.ip,
            riskScore = ipRisk.abuseConfidenceScore.toFloat(),
            isBlocked = ipRisk.isRisky,
            country = ipRisk.country,
            isp = ipRisk.isp
        )
        val ipRef = firestore.collection(FirestoreCollection.IP.value).add(ipLog).await()

        val transaction = FirestoreTransactions(
            payerUserId = payer.userId,
            payerDeviceId = Settings.Secure.getString(getApplication<Application>().contentResolver, Settings.Secure.ANDROID_ID)
                ?: UUID.randomUUID().toString(),
            payerIFSC = payer.bankIFSC,
            payerVpa = payer.bankVPA,
            receiverUserId = receiver.userId,
            receiverVpa = receiver.bankVPA,
            receiverIfsc = receiver.bankIFSC,
            amount = amount,
            status = status,
            locationLogId = locationRef.id,
            ipLogId = ipRef.id,
            fraudScore = modelOutput.fraud_probability,
            ipRiskScore = ipRisk.abuseConfidenceScore.toFloat(),
            locationRiskScore = deviation.toFloat(),
            modelDecision = modelOutput.is_fraud,
            razorpayOrderId = razorpayOrderId,
            razorpayPaymentId = razorpayPaymentId
        )
        val txRef = firestore.collection(FirestoreCollection.TRANSACTIONS.value).add(transaction).await()
        return txRef.id
    }

    private suspend fun resolvePayer(userIdOrVpa: String): FirestoreUser? {
        if (userIdOrVpa.isBlank()) return null
        val byDoc = firestore.collection(FirestoreCollection.USER.value).document(userIdOrVpa).get().await().toObject<FirestoreUser>()
        if (byDoc != null) return byDoc
        val snap = firestore.collection(FirestoreCollection.USER.value)
            .whereEqualTo("bankVPA", userIdOrVpa.trim())
            .limit(1)
            .get()
            .await()
        return snap.documents.firstOrNull()?.toObject()
    }

    private suspend fun getRecipient(recipientVpa: String, payerUserId: String): FirestoreUser? {
        if (recipientVpa.isNotBlank()) {
            val snap = firestore.collection(FirestoreCollection.USER.value)
                .whereEqualTo("bankVPA", recipientVpa)
                .limit(1)
                .get()
                .await()
            return snap.documents.firstOrNull()?.toObject()
        }

        val snap = firestore.collection(FirestoreCollection.USER.value)
            .limit(30)
            .get()
            .await()
        return snap.documents
            .mapNotNull { it.toObject(FirestoreUser::class.java) }
            .firstOrNull { it.userId != payerUserId }
    }

    private suspend fun fetchPublicIp(): String? = withContext(Dispatchers.IO) {
        try {
            URL("https://icanhazip.com/").openConnection().run {
                inputStream.bufferedReader().readText().trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) return@withContext null
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.await()
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun checkIpRisk(ip: String): AbuseRiskResult? {
        return try {
            val response = RetrofitInstance.IPDB_api.checkIP(ip = ip)
            if (response.isSuccessful) {
                response.body()?.data?.let { IPAbuseRiskHelper.evaluateRisk(it) }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun computeDeviationFromLastTransaction(
        payerUserId: String,
        currentLocation: Pair<Double, Double>
    ): Double {
        val lastTx = firestore.collection(FirestoreCollection.TRANSACTIONS.value)
            .whereEqualTo("payerUserId", payerUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
            .toObjects(FirestoreTransactions::class.java)
            .firstOrNull()
            ?: return 0.0

        val locationId = lastTx.locationLogId
        if (locationId.isBlank()) return 0.0

        val lastLoc = firestore.collection(FirestoreCollection.LOCATION.value)
            .document(locationId)
            .get()
            .await()
            .toObject(FirestoreLocationLog::class.java)
            ?: return 0.0

        return haversineKm(
            lastLoc.latitude to lastLoc.longitude,
            currentLocation
        )
    }

    private fun haversineKm(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(b.first - a.first)
        val dLon = Math.toRadians(b.second - a.second)
        val aa = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(a.first)) * cos(Math.toRadians(b.first)) *
            sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(aa), sqrt(1 - aa))
        return earthRadiusKm * c
    }

    private fun defaultModelOutput(): ModelOutput {
        return ModelOutput(
            txn_id = UUID.randomUUID().toString(),
            is_fraud = false,
            fraud_probability = 0.0f,
            risk_level = "LOW"
        )
    }

    private fun appendResult(result: DemoScenarioResult) {
        _uiState.value = _uiState.value.copy(results = _uiState.value.results + result)
        addLog("${result.scenario.label}: ${result.transactionStatus.uppercase()}")
    }

    private fun addLog(text: String) {
        _uiState.value = _uiState.value.copy(logLines = _uiState.value.logLines + text)
    }
}
