package com.example.fraudlens.ui.screens


import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background

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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.ui.theme.*
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
//import com.example.fraudlens.viewmodel.PaymentViewModel


@Composable
fun CreateBankAccount(
    navController: NavController,
    viewModel: FirestorePaymentViewModel
) {
    val context = LocalContext.current

    var vpa by remember { mutableStateOf("") }
    var ifsc by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }

    val textFieldColors = TextFieldDefaults.colors(
        unfocusedTextColor = colorScheme.onSurface,
        focusedTextColor = colorScheme.onPrimary,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedContainerColor = colorScheme.surfaceVariant,
        focusedContainerColor = colorScheme.primaryContainer,
        cursorColor = colorScheme.primary,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss on outside tap */ },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        navController.navigate(Screen.biometricCheck.route)
                    }
                ) {
                    Text("Proceed")
                }
            },
            title = {
                Text("Account Created", color = colorScheme.primary)
            },
            text = {
                Text("Your account has been created successfully.")
            },
            containerColor = colorScheme.surface,
            titleContentColor = colorScheme.onSurface,
            textContentColor = colorScheme.onSurface
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(30.dp)
    ) {
        Column {
            Text("Connect", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
            Text("Your Bank", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Ensure safe transactions from now on!",
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(40.dp))

            TextField(
                value = vpa,
                onValueChange = { vpa = it },
                label = { Text("Enter UPI ID (VPA)") },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextField(
                value = ifsc,
                onValueChange = { ifsc = it },
                label = { Text("Enter Bank IFSC") },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Initial Amount") },
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            TextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number +91") },
                colors = textFieldColors,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val balance = amount.toDoubleOrNull()
                    if (vpa.isNotBlank() && ifsc.isNotBlank() && isValidCredentials(vpa,ifsc) && balance != null && phone.length==10) {
                            viewModel.completeAccountSetup(vpa,ifsc,balance,phone){ flag,result->
                                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                                if(flag){
                                    showDialog = true
                                }else{
                                    navController.navigate(Screen.signup.route)
                                }
                            }
                    }
                    else if(!isValidCredentials(vpa,ifsc)){
                        Toast.makeText(context, "Please enter valid VPA and IFSC", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                )
            ) {
                Text("Join Now")
            }
        }
    }
}

fun isValidCredentials(vpa: String,ifsc: String): Boolean {
    val vpaRegex = Regex("^[a-zA-Z0-9\\.\\-_]{2,256}@[a-zA-Z]{2,64}$")
    val ifscRegex = Regex("^[A-Z]{4}0[A-Z0-9]{6}$")
    return vpaRegex.matches(vpa) && ifscRegex.matches(ifsc)
}
