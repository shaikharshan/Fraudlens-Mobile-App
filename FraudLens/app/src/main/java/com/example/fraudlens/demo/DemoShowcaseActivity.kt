package com.example.fraudlens.demo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fraudlens.ui.components.BiometricPromptManager
import com.example.fraudlens.ui.components.RazorpayPaymentManager
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.ui.screens.BiometricScreen
import com.example.fraudlens.ui.screens.CreateBankAccount
import com.example.fraudlens.ui.screens.Home
import com.example.fraudlens.ui.screens.IpLocationCaptureScreen
import com.example.fraudlens.ui.screens.LiveDetectionScreen
import com.example.fraudlens.ui.screens.ProfileScreen
import com.example.fraudlens.ui.screens.ScamCheckerScreen
import com.example.fraudlens.ui.screens.SendMoneyScreen
import com.example.fraudlens.ui.screens.SignIn
import com.example.fraudlens.ui.screens.SignUp
import com.example.fraudlens.ui.theme.FraudLensTheme
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Full app navigation (same routes as [com.example.fraudlens.MainActivity]) with a floating
 * judge-demo panel. Use it to queue [DemoAutomationBridge] prefill/overrides, then navigate —
 * Send Money will read them like "Selenium-filled" fields.
 */
@AndroidEntryPoint
class DemoShowcaseActivity : AppCompatActivity(), PaymentResultListener {

    private lateinit var razorpayManager: RazorpayPaymentManager

    private val promptManager by lazy { BiometricPromptManager(this) }

    @SuppressLint("UnrememberedGetBackStackEntry")
    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        razorpayManager = RazorpayPaymentManager(this)
        enableEdgeToEdge()
        setContent {
            FraudLensTheme {
                val navController = rememberNavController()
                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.login.route,
                        route = Screen.root.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        composable(Screen.login.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            SignIn(navController, viewModel)
                        }
                        composable(Screen.signup.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            SignUp(navController, viewModel)
                        }
                        composable(Screen.createAccount.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            CreateBankAccount(navController, viewModel)
                        }
                        composable(Screen.biometricCheck.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            BiometricScreen(navController, promptManager, viewModel)
                        }
                        composable(Screen.sendMoney.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            SendMoneyScreen(navController = navController, viewModel = viewModel)
                        }
                        composable(Screen.locationIP.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            IpLocationCaptureScreen(viewModel, navController)
                        }
                        composable(Screen.home.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            Home(viewModel, navController)
                        }
                        composable(Screen.profile.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.root.route) }
                            val viewModel = hiltViewModel<FirestorePaymentViewModel>(parentEntry)
                            ProfileScreen(viewModel, navController)
                        }
                        composable(Screen.smsFraudCheck.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.smsFraudCheck.route) }
                            ScamCheckerScreen(navController)
                        }
                        composable(Screen.liveDetection.route) {
                            val parentEntry =
                                remember { navController.getBackStackEntry(Screen.liveDetection.route) }
                            LiveDetectionScreen(navController)
                        }
                    }

                    DemoShowcaseOverlay(
                        navController = navController,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
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

@Composable
private fun DemoShowcaseOverlay(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var recipientVpa by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("101") }
    var riskyIp by remember { mutableStateOf("") }
    var farLat by remember { mutableStateOf("40.7128") }
    var farLon by remember { mutableStateOf("-74.0060") }
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Judge showcase", fontWeight = FontWeight.Bold)
                Button(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            if (expanded) {
                Text(
                    "Log in as usual, capture IP & location once, then use the buttons below. " +
                        "Values are injected into Send Money (same as typing + overrides).",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = recipientVpa,
                    onValueChange = { recipientVpa = it },
                    label = { Text("Recipient VPA") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = riskyIp,
                    onValueChange = { riskyIp = it },
                    label = { Text("Risky IP override (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = farLat,
                        onValueChange = { farLat = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Far lat") }
                    )
                    OutlinedTextField(
                        value = farLon,
                        onValueChange = { farLon = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Far lon") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        DemoAutomationBridge.clearAll()
                        DemoAutomationBridge.queueSendMoneyPrefill(recipientVpa, amount)
                        navController.navigate(Screen.sendMoney.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recipientVpa.isNotBlank()
                ) { Text("1) Normal: prefill only → Send Money") }
                Button(
                    onClick = {
                        DemoAutomationBridge.clearAll()
                        DemoAutomationBridge.queueSendMoneyPrefill(recipientVpa, amount)
                        riskyIp.takeIf { it.isNotBlank() }?.let {
                            DemoAutomationBridge.queueIpLocationOverrides(it, null)
                        }
                        navController.navigate(Screen.sendMoney.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recipientVpa.isNotBlank() && riskyIp.isNotBlank()
                ) { Text("2a) Risky IP override → Send Money") }
                Button(
                    onClick = {
                        DemoAutomationBridge.clearAll()
                        DemoAutomationBridge.queueSendMoneyPrefill(recipientVpa, amount)
                        val lat = farLat.toDoubleOrNull()
                        val lon = farLon.toDoubleOrNull()
                        if (lat != null && lon != null) {
                            DemoAutomationBridge.queueIpLocationOverrides(null, lat to lon)
                        }
                        navController.navigate(Screen.sendMoney.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recipientVpa.isNotBlank() && farLat.toDoubleOrNull() != null && farLon.toDoubleOrNull() != null
                ) { Text("2b) Far location override → Send Money") }
                Button(
                    onClick = {
                        DemoAutomationBridge.clearAll()
                        DemoAutomationBridge.queueSendMoneyPrefill(recipientVpa, amount)
                        val lat = farLat.toDoubleOrNull()
                        val lon = farLon.toDoubleOrNull()
                        DemoAutomationBridge.queueIpLocationOverrides(
                            riskyIp.takeIf { it.isNotBlank() },
                            if (lat != null && lon != null) lat to lon else null
                        )
                        navController.navigate(Screen.sendMoney.route) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recipientVpa.isNotBlank()
                ) { Text("2) Risky IP + far location → Send Money") }
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        navController.navigate(Screen.locationIP.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Capture IP & Location") }
            }
        }
    }
}
