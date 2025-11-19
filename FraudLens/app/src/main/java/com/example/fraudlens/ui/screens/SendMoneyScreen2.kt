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

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fraudlens.data.local.entities.DEVIATION
import com.example.fraudlens.data.local.entities.FLAG_COUNTDOWN_SEC
import com.example.fraudlens.data.local.entities.FirestoreCollection
import com.example.fraudlens.data.local.entities.FirestoreUser
import com.example.fraudlens.data.local.entities.TransactionResponse

import com.example.fraudlens.retrofit.AbuseRiskResult
import com.example.fraudlens.ui.components.BiometricPromptManager
import com.example.fraudlens.ui.components.BiometricPromptManager.BiometricResult
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
import com.example.fraudlens.viewmodel.RetrofitViewModel
import kotlinx.coroutines.CoroutineScope
//import com.example.fraudlens.viewmodel.PaymentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun SendMoneyScreen2(
    viewModel: FirestorePaymentViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var recipientVPA by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var recipientInfo by remember { mutableStateOf<FirestoreUser?>(null) }
    var showRecipientTooltip by remember { mutableStateOf(false) }
    val location = viewModel._transactionLocation
    val ip = viewModel._transactionIP



    //IP check and location check
    val retrofitViewModel = remember { RetrofitViewModel() }
    val coroutineScope = rememberCoroutineScope()
    var showIPRiskDialog by remember { mutableStateOf(false) }
    val promptManager = remember { BiometricPromptManager(context as AppCompatActivity) }
    val ipRiskResult by retrofitViewModel.ipRiskResult.collectAsState()
    var deviationFromLast = viewModel.deviationFromLast.collectAsStateWithLifecycle()

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
                    onConfirm2(currentUser!!, recipientInfo, amountInput.toDoubleOrNull()!!, viewModel, ipRiskResult!!,location.value,
                        TransactionResponse.APPROVED.value) { result, flag ->
                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                        if (flag) {
                            navController.navigate(Screen.home.route)
                        } else {
                            navController.popBackStack()
                        }
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
                            if(pin == currentUser.password && amount < currentUser.balance){
                                Log.d("ipRiskResult", ipRiskResult.toString())
                                if ((ipRiskResult != null && ipRiskResult?.isRisky!!) || isLocationRisky2(deviationFromLast.value)) {
                                    showIPRiskDialog = true
                                }
                                else if(ipRiskResult==null){
                                    Toast.makeText(context, "Failed to verify network security. Try again later", Toast.LENGTH_SHORT).show()
                                }
                                else {
                                    // Safe, proceed with transaction
                                    showConfirmationDialog = false
                                    onConfirm2(currentUser, recipientInfo, amount, viewModel,ipRiskResult!!,location.value,
                                        TransactionResponse.APPROVED.value) { result, flag ->
                                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                        if (flag) {
                                            navController.navigate(Screen.home.route) {
                                                popUpTo(Screen.sendMoney.route) { inclusive = true }
                                            }
                                        } else {
                                            navController.popBackStack()
                                        }
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
                        )
                    ) {
                        Text("Confirm Payment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmationDialog = false }) {
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
                                .padding(vertical = 4.dp)
                        )

                    }
                }
            )
            if (showIPRiskDialog && ipRiskResult != null) {

                var countdown by remember { mutableStateOf(FLAG_COUNTDOWN_SEC) }
                var canRetry by remember { mutableStateOf(true) }

                // Start countdown
                LaunchedEffect(showIPRiskDialog) {
                    while (countdown > 0) {
                        delay(1000L)
                        countdown--
                    }
                    // Auto-cancel when timer ends
                    if (countdown == 0) {
                        canRetry = false
                        showIPRiskDialog = false

                        // Call onConfirm2 with BLOCKED status
                        onConfirm2(
                            currentUser,
                            recipientInfo,
                            amount,
                            viewModel,
                            ipRiskResult!!,
                            location.value,
                            TransactionResponse.BLOCKED.value
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
                                    showIPRiskDialog = false
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
                            showIPRiskDialog = false
                            onConfirm2(
                                currentUser,
                                recipientInfo,
                                amount,
                                viewModel,
                                ipRiskResult!!,
                                location.value,
                                TransactionResponse.BLOCKED.value
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
                                Text("Your network has been flagged for abuse. This transaction has been flagged for your safety.")
                                Spacer(Modifier.height(8.dp))
                                Text("Country: ${ipRiskResult?.country!!}")
                                Text("ISP: ${ipRiskResult?.isp}")
                                Spacer(Modifier.height(8.dp))
                                ipRiskResult?.reasons?.forEach {
                                    Text("• $it", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (isLocationRisky2(deviationFromLast.value)) {
                                Text("Your location has been found to be beyond 100 km of your last transaction. This transaction has been flagged for your safety.")
                                Spacer(Modifier.height(8.dp))
                                Text("Deviation (km): ${deviationFromLast.value}")
                                Spacer(Modifier.height(8.dp))
                                Text("Please verify with your fingerprint to continue.")
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "You have $countdown seconds to respond.",
                                style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.error)
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.error,
                    textContentColor = MaterialTheme.colorScheme.onSurface
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
                            RecipientItem2(user = user) {
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

fun isLocationRisky2(deviationFromLast: Double): Boolean{

    //distance between current and last transactions is greater than 100
    return (deviationFromLast >= DEVIATION)
}


@OptIn(ExperimentalCoroutinesApi::class)
fun onConfirm2(
    payer: FirestoreUser,
    receiver: FirestoreUser?,
    amount: Double,
    viewModel: FirestorePaymentViewModel,
    ipRiskResult: AbuseRiskResult,
    location: Pair<Double, Double>?,
    status: String,
    confirmMsg : (result: String,flag: Boolean) -> Unit
) {
    val device =  viewModel.currentDevice.value
    val deviationFromLast = viewModel.deviationFromLast.value

    try{
        if(device != null && receiver !=null && location != null){

            viewModel.addTransaction(
                payerUserId = payer.userId,
                payerVpa = payer.bankVPA,
                payerIFSC = payer.bankIFSC,
                payerDeviceId = device.deviceId,
                receiverVpa = receiver.bankVPA,
                receiverIfsc = receiver.bankIFSC,
                amount = amount,
                fraudScore = 0F,
                locationRiskScore = 0F,
                modelDecision = false,
                status = status,
                ipRiskResult = ipRiskResult,
                location = location,
                deviation = deviationFromLast

            )
            if(status == TransactionResponse.APPROVED.value){
                viewModel.updateUserBalance(amount,receiver.userId)
                viewModel.loadTransactions(payer.userId)
                confirmMsg("Amount $amount sent successfully to ${receiver.bankVPA}",true)
            }
            else{
                confirmMsg("Transaction blocked successfully, thanks to our secure system.",false)
            }

        }
        else{
            confirmMsg("Error while processing transaction. Try again later",false)
            Log.d("CustomException",device.toString())
            Log.d("CustomException",receiver.toString())

        }
    }catch (e: Exception){
        Log.d("CustomException",e.toString())
    }



}

@Composable
fun RecipientItem2(user: FirestoreUser, onClick: () -> Unit) {
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
