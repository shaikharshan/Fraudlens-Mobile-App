package com.example.fraudlens

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fraudlens.ui.components.BiometricPromptManager
import com.example.fraudlens.ui.components.RazorpayPaymentManager
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.ui.screens.BiometricScreen
import com.example.fraudlens.ui.screens.*
import com.example.fraudlens.ui.screens.SignIn
import com.example.fraudlens.ui.screens.SignUp
import com.example.fraudlens.ui.theme.FraudLensTheme
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
import com.razorpay.PaymentResultListener
//import com.example.fraudlens.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
class MainActivity : AppCompatActivity(),PaymentResultListener {

    companion object {
        var razorpayManager: RazorpayPaymentManager? = null
    }


    private val promptManager by lazy {
        BiometricPromptManager(this)
    }


    @SuppressLint("UnrememberedGetBackStackEntry")
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        razorpayManager = RazorpayPaymentManager(this)

        enableEdgeToEdge()
        setContent {
            FraudLensTheme {

                Spacer(Modifier.height(20.dp))
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screen.login.route,
                    route = Screen.root.route,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
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


            }
        }

    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        razorpayManager?.handlePaymentSuccess(razorpayPaymentId)
    }

    override fun onPaymentError(code: Int, response: String?) {
        razorpayManager?.handlePaymentError(code, response)
    }


}

