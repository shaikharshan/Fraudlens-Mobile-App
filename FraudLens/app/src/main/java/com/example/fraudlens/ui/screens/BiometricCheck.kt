package com.example.fraudlens.ui.screens

import android.content.Intent

import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fraudlens.ui.components.BiometricPromptManager
import com.example.fraudlens.ui.components.BiometricPromptManager.BiometricResult
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel

@Composable
fun BiometricScreen(
    navController: NavController,
    promptManager: BiometricPromptManager,
    viewModel: FirestorePaymentViewModel
) {
    val biometricResult by promptManager.promptResults.collectAsState(initial = null)
    var showWarningDialog by remember { mutableStateOf(false) }
    var triesLeft by remember{mutableStateOf(3)}
    var allowDeviceCredential by remember { mutableStateOf(false) }

    val enrollLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            // Can optionally handle result
        }
    )

    LaunchedEffect(biometricResult) {
        when (biometricResult) {
            BiometricResult.AuthenticationSuccess -> {
                viewModel.setBiometricFlag(true)
                navController.navigate(Screen.locationIP.route)
            }
            is BiometricResult.AuthenticationFailed ->{
                triesLeft--
                if(triesLeft<0){
                    promptManager.showBiometricPrompt(
                        title = "Use Device Credential",
                        description = "Use fingerprint or device password",
                        useDeviceCredentials = true  // <-- fallback here
                    )
                }
            }

            is BiometricResult.AuthenticationNotSet -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                        )
                    }
                    enrollLauncher.launch(enrollIntent)
                }
            }

            BiometricResult.FeatureUnavailable, BiometricResult.HardwareUnavailable -> {
                showWarningDialog = true
            }

            else -> Unit
        }
    }

    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = {
                    viewModel.setBiometricFlag(false)
                    navController.navigate(Screen.locationIP.route)
                }) {
                    Text("Continue")
                }
            },
            title = {
                Text("Biometric Not Supported")
            },
            text = {
                Text("This device does not support biometric authentication. The app may not function fully without it.")
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = Icons.Default.Lock,
                contentDescription = "Fingerprint Icon",
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Secure Login",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Use your fingerprint to proceed securely",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    promptManager.showBiometricPrompt(
                        title = "Biometric Authentication",
                        description = "Use your fingerprint to continue"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Authenticate")
            }

            Spacer(modifier = Modifier.height(24.dp))

            biometricResult?.let { result ->
                Text(
                    text = when (result) {
                        is BiometricResult.AuthenticationError -> result.error
                        BiometricResult.AuthenticationFailed -> "Authentication failed. Try again."
                        BiometricResult.AuthenticationSuccess -> "Success"
                        BiometricResult.AuthenticationNotSet -> "No biometric enrolled. Please set it up."
                        BiometricResult.FeatureUnavailable -> "Biometric feature not available."
                        BiometricResult.HardwareUnavailable -> "Biometric hardware unavailable."
                        else -> ""
                    },
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

