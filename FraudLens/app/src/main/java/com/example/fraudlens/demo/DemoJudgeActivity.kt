package com.example.fraudlens.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fraudlens.retrofit.RazorpayPaymentResult
import com.example.fraudlens.ui.components.RazorpayPaymentManager
import com.example.fraudlens.ui.theme.FraudLensTheme
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.CompletableDeferred

class DemoJudgeActivity : ComponentActivity(), PaymentResultListener {
    private val vm by viewModels<DemoJudgeViewModel>()
    private lateinit var razorpayManager: RazorpayPaymentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        razorpayManager = RazorpayPaymentManager(this)
        enableEdgeToEdge()
        setContent {
            FraudLensTheme {
                DemoJudgeApp(
                    vm = vm,
                    launchRazorpayCheckout = { orderId, amount, payerName, receiverName, email, phone ->
                        val signal = CompletableDeferred<RazorpayPaymentResult>()
                        vm.attachPaymentSignal(signal)
                        razorpayManager.startPayment(
                            orderId = orderId,
                            amount = amount,
                            name = payerName,
                            description = "Demo transfer to $receiverName",
                            userEmail = email,
                            userPhone = phone,
                            onSuccess = { signal.complete(it) },
                            onFailure = { signal.complete(it) }
                        )
                    }
                )
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        razorpayManager.handlePaymentSuccess(razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, response: String?) {
        razorpayManager.handlePaymentError(code, response)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoJudgeApp(
    vm: DemoJudgeViewModel,
    launchRazorpayCheckout: suspend (orderId: String, amount: Double, payerName: String, receiverName: String, email: String?, phone: String?) -> Unit
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var localConfig by remember(uiState.config) { mutableStateOf(uiState.config) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("FraudLens Judge Demo", fontWeight = FontWeight.Bold)
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Demo Setup", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = localConfig.payerUserId,
                            onValueChange = { localConfig = localConfig.copy(payerUserId = it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Payer (Firestore user id or bank VPA)") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = localConfig.recipientVpa,
                            onValueChange = { localConfig = localConfig.copy(recipientVpa = it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Recipient VPA (optional)") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = localConfig.riskyIpForOverride,
                            onValueChange = { localConfig = localConfig.copy(riskyIpForOverride = it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Risky IP override") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = localConfig.farLatitude.toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        localConfig = localConfig.copy(farLatitude = value)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Far latitude") }
                            )
                            OutlinedTextField(
                                value = localConfig.farLongitude.toString(),
                                onValueChange = {
                                    it.toDoubleOrNull()?.let { value ->
                                        localConfig = localConfig.copy(farLongitude = value)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Far longitude") }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.onConfigChange(localConfig) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Apply Config") }
                            Button(
                                onClick = {
                                    vm.onConfigChange(localConfig)
                                    vm.startDemo { orderId, amount, payer, receiver ->
                                        launchRazorpayCheckout(
                                            orderId,
                                            amount,
                                            payer.username,
                                            receiver.username,
                                            payer.email,
                                            payer.phone
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = uiState.runnerStatus != DemoRunnerStatus.RUNNING
                            ) { Text("Start Demo") }
                        }
                        if (uiState.awaitingNext) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { vm.continueAfterPause() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Next Scenario")
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Runner", fontWeight = FontWeight.SemiBold)
                        Text("Status: ${uiState.runnerStatus}")
                        Text("Current: ${uiState.currentScenario?.label ?: "-"}")
                        if (uiState.countdownSeconds > 0) {
                            Text("Auto-block countdown: ${uiState.countdownSeconds}s")
                        }
                        uiState.latestError?.let {
                            Text("Error: $it", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                Text("Scenario Results", fontWeight = FontWeight.SemiBold)
            }
            items(uiState.results) { result ->
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(result.scenario.label, fontWeight = FontWeight.Bold)
                        Text("Decision: ${result.transactionStatus.uppercase()}")
                        Text(result.statusText)
                        if (result.transactionIdHint.isNotBlank()) {
                            Text("Transaction ID: ${result.transactionIdHint}")
                        }
                        result.locationDeviationKm?.let { Text("Deviation: ${"%.2f".format(it)} km") }
                        result.ipRisk?.let { Text("IP Risk: ${it.isRisky} | Score: ${it.abuseConfidenceScore}") }
                        result.modelOutput?.let { Text("ML: fraud=${it.is_fraud}, p=${it.fraud_probability}") }
                    }
                }
            }

            item {
                Text("Realtime Logs", fontWeight = FontWeight.SemiBold)
            }
            items(uiState.logLines) { line ->
                Text(line, style = MaterialTheme.typography.bodySmall)
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
