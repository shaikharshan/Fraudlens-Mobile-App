package com.example.fraudlens.ui.screens


import android.util.Log
import kotlinx.coroutines.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import java.util.UUID
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.fraudlens.MainActivity
import com.example.fraudlens.data.local.entities.DEVIATION
import com.example.fraudlens.data.local.entities.FLAG_COUNTDOWN_SEC
import com.example.fraudlens.data.local.entities.FirestoreCollection
import com.example.fraudlens.data.local.entities.FirestoreUser
import com.example.fraudlens.data.local.entities.TransactionResponse

import com.example.fraudlens.retrofit.AbuseRiskResult
import com.example.fraudlens.retrofit.ModelOutput
import com.example.fraudlens.ui.components.BiometricPromptManager
import com.example.fraudlens.ui.components.BiometricPromptManager.BiometricResult
import com.example.fraudlens.ui.components.RazorpayPaymentManager
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
import com.example.fraudlens.viewmodel.RazorpayViewModel
import com.example.fraudlens.viewmodel.RetrofitViewModel

import kotlinx.coroutines.CoroutineScope
//import com.example.fraudlens.viewmodel.PaymentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun SendMoneyScreen(
    viewModel: FirestorePaymentViewModel,
    navController: NavController,
    razorpayViewModel: RazorpayViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Use the manager from MainActivity
    val razorpayPaymentManager = remember {
        MainActivity.razorpayManager ?: RazorpayPaymentManager(context as AppCompatActivity)
    }


    var recipientVPA by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var recipientInfo by remember { mutableStateOf<FirestoreUser?>(null) }
    var showRecipientTooltip by remember { mutableStateOf(false) }
    val location = viewModel._transactionLocation
    val ip = viewModel._transactionIP

    //razorpay
    var currentOrderId by remember { mutableStateOf<String?>(null) }

    //IP check and location check
    val retrofitViewModel = remember { RetrofitViewModel() }
    val coroutineScope = rememberCoroutineScope()
    var showRiskDialog by remember { mutableStateOf(false) }
    val promptManager = remember { BiometricPromptManager(context as AppCompatActivity) }
    val ipRiskResult by retrofitViewModel.ipRiskResult.collectAsState()
    var deviationFromLast = viewModel.deviationFromLast.collectAsStateWithLifecycle()

    //model check
    val modelOutput = retrofitViewModel.modelPrediction.collectAsStateWithLifecycle().value

    var showSearchList by remember { mutableStateOf(false) }
    var pin by remember{ mutableStateOf("")}

    val currentUser = viewModel.loggedUser.collectAsStateWithLifecycle().value
    val currentDevice = viewModel.currentDevice.collectAsState().value
    val searchUser = viewModel.searchUsers.collectAsState().value
    val focusRequester = remember { FocusRequester() }

    val textFieldColors = TextFieldDefaults.colors(
        unfocusedTextColor = colorScheme.onSurface,
        focusedTextColor = colorScheme.primary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedContainerColor = colorScheme.surfaceVariant,
        focusedContainerColor = colorScheme.primaryContainer,
        cursorColor = colorScheme.primary,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    LaunchedEffect(promptManager.promptResults) {
        promptManager.promptResults.collect { result ->
            when (result) {
                is BiometricResult.AuthenticationSuccess -> {
                    showConfirmationDialog = false
                    showRiskDialog = false

                    val amount = amountInput.toDoubleOrNull()
                    if (amount != null && currentUser != null && recipientInfo != null && ipRiskResult != null && location.value != null) {
                        Log.d("BiometricSuccess", "Fingerprint verified, proceeding with Razorpay payment")

                        // Create Razorpay order after biometric success
                        val uuid = UUID.randomUUID().toString()
                        val razorpayReceipt = uuid

                        razorpayViewModel.createOrder(
                            amount = amount,
                            receipt = razorpayReceipt,
                            notes = mapOf(
                                "payer_vpa" to currentUser.bankVPA,
                                "receiver_vpa" to recipientInfo!!.bankVPA,
                                "transaction_type" to "transfer_risky"
                            ),
                            onSuccess = { orderResponse ->
                                currentOrderId = orderResponse.id
                                Log.d("BiometricSuccess", "Order created: ${orderResponse.id}")

                                // Launch Razorpay checkout
                                razorpayPaymentManager.startPayment(
                                    orderId = orderResponse.id,
                                    amount = amount,
                                    name = currentUser.username,
                                    description = "Payment to ${recipientInfo!!.username}",
                                    userEmail = currentUser.email,
                                    userPhone = currentUser.phone,
                                    onSuccess = { paymentResult ->
                                        Log.d("BiometricSuccess", "Payment successful: ${paymentResult.paymentId}")

                                        // Payment successful - record transaction
                                        onConfirm(
                                            currentUser,
                                            recipientInfo,
                                            amount,
                                            viewModel,
                                            ipRiskResult!!,
                                            location.value,
                                            TransactionResponse.APPROVED.value,
                                            retrofitViewModel,
                                            razorpayOrderId = orderResponse.id,
                                            razorpayPaymentId = paymentResult.paymentId
                                        ) { result, flag ->
                                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                            if (flag) {
                                                navController.navigate(Screen.home.route) {
                                                    popUpTo(Screen.sendMoney.route) { inclusive = true }
                                                }
                                            }
                                        }
                                    },
                                    onFailure = { paymentResult ->
                                        Log.e("BiometricSuccess", "Payment failed: ${paymentResult.errorMessage}")
                                        Toast.makeText(
                                            context,
                                            "Payment failed: ${paymentResult.errorMessage}",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        // Record failed transaction
                                        onConfirm(
                                            currentUser,
                                            recipientInfo,
                                            amount,
                                            viewModel,
                                            ipRiskResult!!,
                                            location.value,
                                            TransactionResponse.BLOCKED.value,
                                            retrofitViewModel,
                                            razorpayOrderId = orderResponse.id,
                                            razorpayPaymentId = null
                                        ) { _, _ -> }
                                    }
                                )
                            },
                            onError = { error ->
                                Log.e("BiometricSuccess", "Order creation failed: $error")
                                Toast.makeText(
                                    context,
                                    "Failed to create payment order: $error",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else {
                        Log.e("BiometricSuccess", "Missing required data after fingerprint verification")
                        Toast.makeText(context, "Error: Missing transaction data", Toast.LENGTH_SHORT).show()
                    }
                }
                is BiometricResult.AuthenticationFailed -> {
                    Toast.makeText(context, "Fingerprint failed. Transaction blocked.", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            focusRequester.requestFocus()
        }

        if (ip.value != null) {
//            ip.value = "160.19.205.25"
            retrofitViewModel.checkRisk(ip.value!!)
        }

        if(location.value != null){
            viewModel.checkLocationRisk()
        }
        retrofitViewModel.checkHealth()
    }

    LaunchedEffect(recipientVPA) {
        if (recipientVPA.isNotBlank() ) {
//            viewModel.getUsersByName("%"+recipientVPA+"%") for SQL room DB
            viewModel.getUsersByName(recipientVPA)

            Log.d("vpaSearch",searchUser.toString())
            showSearchList = true
        }
        else if(recipientVPA.isBlank()){
            showSearchList = false
        }
    }

    if (showConfirmationDialog) {
        val amount = amountInput.toDoubleOrNull()

        if (currentUser!=null && recipientInfo!=null &&  amount != null && recipientInfo != null) {
            AlertDialog(
                onDismissRequest = { showConfirmationDialog = false },
                confirmButton = {
                    Button(
                        onClick = {
                            if(pin == currentUser.password && amount < currentUser.balance) {
                                Log.d("ipRiskResult", ipRiskResult.toString())
                                showLoading = true

                                coroutineScope.launch {
                                    try {
                                        val isRisky = checkIfTransactionIsRisky(
                                            ipRiskResult = ipRiskResult,
                                            locationDeviation = deviationFromLast.value,
                                            receiver = recipientInfo!!,
                                            viewModel = viewModel,
                                            retrofitViewModel = retrofitViewModel,
                                            amount = amount
                                        )

                                        showLoading = false

                                        if (isRisky) {
                                            showRiskDialog = true
                                        } else if (ipRiskResult == null) {
                                            Toast.makeText(
                                                context,
                                                "Failed to verify network security. Try again later",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // Safe, proceed with transaction
                                            showConfirmationDialog = false

                                            // Create Razorpay order
                                            val uuid = UUID.randomUUID().toString()
                                            val razorpayReceipt = "$uuid"

                                            razorpayViewModel.createOrder(
                                                amount = amount,
                                                receipt = razorpayReceipt,
                                                notes = mapOf(
                                                    "payer_vpa" to currentUser.bankVPA,
                                                    "receiver_vpa" to recipientInfo!!.bankVPA,
                                                    "transaction_type" to "transfer"
                                                ),
                                                onSuccess = { orderResponse ->
                                                    currentOrderId = orderResponse.id

                                                    // Launch Razorpay checkout
                                                    razorpayPaymentManager.startPayment(
                                                        orderId = orderResponse.id,
                                                        amount = amount,
                                                        name = currentUser.username,
                                                        description = "Payment to ${recipientInfo!!.username}",
                                                        userEmail = currentUser.email,
                                                        userPhone = currentUser.phone,
                                                        onSuccess = { paymentResult ->
                                                            // Payment successful - record transaction
                                                            onConfirm(
                                                                currentUser,
                                                                recipientInfo,
                                                                amount,
                                                                viewModel,
                                                                ipRiskResult!!,
                                                                location.value,
                                                                TransactionResponse.APPROVED.value,
                                                                retrofitViewModel,
                                                                razorpayOrderId = orderResponse.id,
                                                                razorpayPaymentId = paymentResult.paymentId
                                                            ) { result, flag ->
                                                                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                                                if (flag) {
                                                                    navController.navigate(Screen.home.route) {
                                                                        popUpTo(Screen.home.route) { inclusive = true }
                                                                    }
                                                                } else {
                                                                    navController.popBackStack()
                                                                }
                                                            }
                                                        },
                                                        onFailure = { paymentResult ->
                                                            // Payment failed
                                                            Toast.makeText(
                                                                context,
                                                                "Payment failed: ${paymentResult.errorMessage}",
                                                                Toast.LENGTH_LONG
                                                            ).show()

                                                            // Record failed transaction
                                                            onConfirm(
                                                                currentUser,
                                                                recipientInfo,
                                                                amount,
                                                                viewModel,
                                                                ipRiskResult!!,
                                                                location.value,
                                                                TransactionResponse.BLOCKED.value,
                                                                retrofitViewModel,
                                                                razorpayOrderId = orderResponse.id,
                                                                razorpayPaymentId = null
                                                            ) { _, _ -> }
                                                        }
                                                    )
                                                },
                                                onError = { error ->
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to create payment order: $error",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        showLoading = false
                                        Log.e("TransactionError", "Error during risk check", e)
                                        Toast.makeText(context, "Error processing transaction. Try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            else if(amount >= currentUser.balance){
                                Toast.makeText(context, "Cannot pay amount greater than balance", Toast.LENGTH_SHORT).show()
                            }
                            else{
                                Toast.makeText(context, "Incorrect Password.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary
                        ),
                        enabled = !showLoading
                    ) {
                        if (showLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirm Payment")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showConfirmationDialog = false },
                        enabled = !showLoading
                    ) {
                        Text("Cancel")
                    }
                },
                title = {
                    Text("Confirm Payment", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text("From: ${currentUser?.username ?: "Unknown"}")
                        Text("To: ${recipientInfo?.username} (${recipientInfo?.bankVPA})")
                        Text("Amount: ₹${"%.2f".format(amount)}")
                        Spacer(Modifier.height(4.dp))
                        TextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("Enter password", color = colorScheme.onSurfaceVariant) },
                            trailingIcon = {
                                val icon = if (passwordVisible) Icons.Filled.Lock else Icons.Filled.Lock
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = colorScheme.onSurface
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !showLoading
                        )

                        if (showLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Analyzing transaction for risk...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )

            // Update the risky dialog to also use Razorpay
            if (showRiskDialog && ipRiskResult != null) {
                var countdown by remember { mutableStateOf(FLAG_COUNTDOWN_SEC) }
                var canRetry by remember { mutableStateOf(true) }

                LaunchedEffect(showRiskDialog) {
                    while (countdown > 0) {
                        delay(1000L)
                        countdown--
                    }
                    if (countdown == 0) {
                        canRetry = false
                        showRiskDialog = false

                        onConfirm(
                            currentUser,
                            recipientInfo,
                            amount,
                            viewModel,
                            ipRiskResult!!,
                            location.value,
                            TransactionResponse.BLOCKED.value,
                            retrofitViewModel,
                            razorpayOrderId = null,
                            razorpayPaymentId = null
                        ) { result, _ ->
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                        }

                        navController.popBackStack()
                    }
                }

                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {
                        if (currentUser.biometricEnabled) {
                            Button(
                                onClick = {
                                    promptManager.showBiometricPrompt(
                                        title = "Verify Identity",
                                        description = "Fingerprint required to proceed on risky network"
                                    )
                                    showRiskDialog = false
                                },
                                enabled = canRetry
                            ) {
                                Text("Retry with Fingerprint (${countdown}s)")
                            }
                        } else {
                            Text("Fingerprint not enabled. Transaction blocked.")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRiskDialog = false
                            onConfirm(
                                currentUser,
                                recipientInfo,
                                amount,
                                viewModel,
                                ipRiskResult!!,
                                location.value,
                                TransactionResponse.BLOCKED.value,
                                retrofitViewModel
                            ) { result, _ ->
                                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                            }
                            navController.popBackStack()
                        }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Transaction Blocked: Risky Network") },
                    text = {
                        Column {
                            if (ipRiskResult?.isRisky!!) {
                                Text("Your network was reported previously for abuse. This transaction has been flagged for safety.")
                                Spacer(Modifier.height(8.dp))
                                Text("Country: ${ipRiskResult?.country!!}")
                                Text("ISP: ${ipRiskResult?.isp}")
                                Spacer(Modifier.height(8.dp))
                                ipRiskResult?.reasons?.forEach {
                                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (isLocationRisky(deviationFromLast.value)) {
                                Text("Your location has been found to be beyond 100 km of your previous transaction. This transaction has been flagged for safety.")
                                Spacer(Modifier.height(8.dp))
                                Text("Deviation (km): ${deviationFromLast.value}")
                                Spacer(Modifier.height(8.dp))
                                Text("Please verify with your fingerprint to continue.")
                            }
                            if(modelOutput!=null && modelOutput.fraud_probability >= 0.5){
                                Text("Our FraudLens AI model flagged this transaction for safety.")
                                Spacer(Modifier.height(8.dp))
                                Text("Risk Level: ${modelOutput.risk_level}")
                                Text("Fraud probability: ${modelOutput.fraud_probability}")
                                Spacer(Modifier.height(8.dp))
                                Text("Please verify with your fingerprint to continue.")
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "You have $countdown seconds to respond.",
                                style = MaterialTheme.typography.labelMedium.copy(color = colorScheme.error)
                            )
                        }
                    },
                    containerColor = colorScheme.surface,
                    titleContentColor = colorScheme.error,
                    textContentColor = colorScheme.onSurface
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Send Money", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column {
            Text("Enter Recipient VPA")
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = recipientVPA,
                onValueChange = { recipientVPA = it },
                label = { Text("e.g., friend@upi") },
                placeholder = { Text("Enter UPI ID") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp),
            )

            if (searchUser.isNotEmpty() && showSearchList) {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(searchUser) { user ->
                        if(user != currentUser){
                            RecipientItem(user = user) {
                                recipientInfo = user.copy()
                                showRecipientTooltip = true
                                showSearchList = false
                                recipientVPA = ""
                            }
                        }
                    }
                }
            }

            if (showRecipientTooltip && recipientInfo != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.LightGray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                recipientInfo!!.username.first().toString(),
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(recipientInfo!!.username, fontWeight = FontWeight.Bold)
                            Text(recipientInfo!!.bankVPA)
                            Text(recipientInfo!!.bankIFSC)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column {
            Text("Enter Amount")
            Spacer(modifier = Modifier.height(8.dp))

            TextField(
                value = amountInput,
                onValueChange = {
                    if (it.isEmpty() || it.toFloatOrNull() != null) {
                        amountInput = it
                    }
                },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text("Available Balance: ₹${currentUser?.balance ?: 0.0}")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val amount = amountInput.toFloatOrNull()
                if (recipientInfo != null && amount != null && amount > 0) {
                    showConfirmationDialog = true
                } else {
                    Toast.makeText(context, "Enter valid recipient and amount", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Money")
        }
    }
}

fun isLocationRisky(deviationFromLast: Double): Boolean{
    //distance between current and last transactions is greater than 100
    return (deviationFromLast >= DEVIATION)
}


@OptIn(ExperimentalCoroutinesApi::class)
fun onConfirm(
    payer: FirestoreUser,
    receiver: FirestoreUser?,
    amount: Double,
    viewModel: FirestorePaymentViewModel,
    ipRiskResult: AbuseRiskResult,
    location: Pair<Double, Double>?,
    status: String,
    retrofitViewModel: RetrofitViewModel,
    razorpayOrderId: String? = null,
    razorpayPaymentId: String? = null,
    confirmMsg : (result: String,flag: Boolean) -> Unit
) {
    Log.d("onConfirm", "=== START onConfirm ===")
    Log.d("onConfirm", "Razorpay OrderID: $razorpayOrderId")
    Log.d("onConfirm", "Razorpay PaymentID: $razorpayPaymentId")
    Log.d("onConfirm", "Status: $status")

    val device = viewModel.currentDevice.value
    val deviationFromLast = viewModel.deviationFromLast.value
    var modelOutput = retrofitViewModel.modelPrediction.value

    Log.d("onConfirm", "Device: ${device?.deviceId}")
    Log.d("onConfirm", "Receiver: ${receiver?.username}")
    Log.d("onConfirm", "Location: $location")
    Log.d("onConfirm", "ModelOutput: $modelOutput")

    try {
        if (device != null && receiver != null && location != null) {

            // If model output is null, create a default one (assume safe)
            if (modelOutput == null) {
                Log.w("onConfirm", "ModelOutput is null, using default values")
                modelOutput = ModelOutput(
                    fraud_probability = 0.0f,
                    is_fraud = false,
                    risk_level = "LOW",
                    txn_id = receiver.userId
                )
            }

            Log.d("onConfirm", "All checks passed, adding transaction...")

            viewModel.addTransaction(
                payerUserId = payer.userId,
                payerVpa = payer.bankVPA,
                payerIFSC = payer.bankIFSC,
                payerDeviceId = device.deviceId,
                receiverVpa = receiver.bankVPA,
                receiverIfsc = receiver.bankIFSC,
                amount = amount,
                fraudScore = modelOutput.fraud_probability,
                locationRiskScore = deviationFromLast.toFloat(),
                modelDecision = modelOutput.is_fraud,
                status = status,
                ipRiskResult = ipRiskResult,
                location = location,
                deviation = deviationFromLast,
                razorpayOrderId = razorpayOrderId,
                razorpayPaymentId = razorpayPaymentId
            )

            if (status == TransactionResponse.APPROVED.value) {
                Log.d("onConfirm", "Transaction APPROVED, updating balances...")
                viewModel.updateUserBalance(amount, receiver.userId)
                viewModel.loadTransactions(payer.userId)
                confirmMsg("Payment of ₹$amount completed successfully to ${receiver.bankVPA}", true)
            } else {
                Log.d("onConfirm", "Transaction BLOCKED")
                confirmMsg("Transaction blocked successfully, thanks to our secure system.", false)
            }
        } else {
            Log.e("onConfirm", "NULL CHECK FAILED!")
            Log.e("onConfirm", "device null: ${device == null}")
            Log.e("onConfirm", "receiver null: ${receiver == null}")
            Log.e("onConfirm", "location null: ${location == null}")

            confirmMsg("Error while processing transaction. Our servers are busy. Try again later", false)
        }
    } catch (e: Exception) {
        Log.e("onConfirm", "EXCEPTION in onConfirm", e)
        confirmMsg("Error: ${e.message}", false)
    }

    Log.d("onConfirm", "=== END onConfirm ===")
}

@Composable
fun RecipientItem(user: FirestoreUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("To: ${user.username}", fontWeight = FontWeight.Bold)
                Text("VPA: ${user.bankVPA}", fontSize = 12.sp)
                Text("Bank IFSC: ${user.bankIFSC}", fontSize = 12.sp)
            }
        }
    }
}

suspend fun checkIfTransactionIsRisky(
    ipRiskResult: AbuseRiskResult?,
    locationDeviation: Double,
    receiver: FirestoreUser,
    viewModel: FirestorePaymentViewModel,
    retrofitViewModel: RetrofitViewModel,
    amount: Double
): Boolean {
    val isIpRisk = ipRiskResult?.isRisky == true
    val isLocationRisk = isLocationRisky(locationDeviation)
    val modelHealth = retrofitViewModel.modelHealth
    Log.d("modelHealth",modelHealth.value.toString())
    Log.d("modelHealth","check if txn risky called")

    val input = viewModel.prepareModelInput(receiver, amount)
    Log.d("modelHealth",input.toString())
    val isModelRisk = if (input != null && modelHealth.value.isNotEmpty()) {
        val result = retrofitViewModel.predictFraud(input)
        Log.d("modelHealth",result.toString())
        result?.is_fraud == true
    } else {
        false
    }

    return isIpRisk || isLocationRisk || isModelRisk
}