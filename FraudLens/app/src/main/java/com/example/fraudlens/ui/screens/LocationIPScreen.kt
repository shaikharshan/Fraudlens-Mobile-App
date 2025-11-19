package com.example.fraudlens.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
//import com.example.fraudlens.viewmodel.PaymentViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalCoroutinesApi::class)
@Composable
fun IpLocationCaptureScreen(viewModel: FirestorePaymentViewModel,
                            navController: NavController) {
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION )
    val context = LocalContext.current
    val ip = viewModel._transactionIP.collectAsState()
    val loc = viewModel._transactionLocation.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {

        Text("Capture IP & Location. Required for secure transactions", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        when {
            !permissionState.status.isGranted -> {
                Text("Location permission needed to proceed")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Grant Location Permission")
                }
            }

            else -> {
                Button(onClick = {
                    viewModel.viewModelScope.launch {
                        val ipVal = viewModel.fetchPublicIPs()
                        viewModel._transactionIP.value = ipVal
//                        viewModel._transactionIP.value = "193.112.146.22"
                        val locVal = viewModel.fetchLocation(context)
                        if(locVal==null){
                            Toast.makeText(context, "Failed to fetch location. Turn on location to proceed.", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        locVal.let {
                            viewModel._transactionLocation.value = it.latitude to it.longitude
                            navController.navigate(Screen.home.route) {
                                popUpTo(Screen.root.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }

                }) {
                    Text("Allow to Fetch IP & Location")

                }

                Spacer(Modifier.height(16.dp))
                Text("IP: ${ip.value ?: "—"}")
                Text("Location: ${loc?.value?.first ?: "-"} , ${loc?.value?.second ?: "-"}")
            }
        }
    }
}
