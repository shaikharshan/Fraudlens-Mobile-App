package com.example.fraudlens.ui.screens


import android.R
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.ui.theme.*
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
//import com.example.fraudlens.viewmodel.PaymentViewModel


//@Preview
@Composable
fun SignUp(
    navController: NavController,
    viewModel: FirestorePaymentViewModel
) {
    val yourTextFieldColors = TextFieldDefaults.colors(
        unfocusedTextColor = colorScheme.onSurface,
        focusedTextColor = colorScheme.onPrimaryContainer,
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        focusedLabelColor = colorScheme.primary,
        unfocusedContainerColor = colorScheme.surfaceVariant,
        focusedContainerColor = colorScheme.primaryContainer,
        cursorColor = colorScheme.primary,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(30.dp)
    ) {
        Column {
            Text("Create", fontSize = 50.sp, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
            Spacer(Modifier.height(10.dp))
            Text("Account", fontSize = 50.sp, fontWeight = FontWeight.Bold, color = colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text("Sign up to get started!", color = colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(40.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = colorScheme.onSurfaceVariant) },
                colors = yourTextFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = colorScheme.onSurfaceVariant) },
                colors = yourTextFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = colorScheme.onSurfaceVariant) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Filled.Lock else Icons.Filled.Lock
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = null, tint = colorScheme.onSurface)
                    }
                },
                colors = yourTextFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(5.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colorScheme.primary,
                        uncheckedColor = colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "I agree to the terms and conditions", color = colorScheme.onSurface)
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && email.isNotBlank() && isValidEmail(email) && password.length >= 6){
                    viewModel.signUpUser(context,name,email,password) { flag,result,num->
                        Toast.makeText(context, "$result and $num", Toast.LENGTH_LONG).show()
                        if(flag){
                            navController.navigate(Screen.createAccount.route)
                        }
                    }
                    }
                    else if (!isValidEmail(email)) {
                        Toast.makeText(context, "Invalid email format", Toast.LENGTH_LONG).show()
                    }
                    else{
                        Toast.makeText(context, "Fill all columns correctly. Password must be >=6 characters", Toast.LENGTH_LONG).show()
                    }
                },

                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Sign Up")
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", color = colorScheme.onSurface)
                Text("Login", color = colorScheme.primary, modifier = Modifier.clickable {
                    navController.popBackStack()
                })
            }
        }
    }
}

fun isValidEmail(email: String): Boolean {
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    return emailRegex.matches(email)
}

