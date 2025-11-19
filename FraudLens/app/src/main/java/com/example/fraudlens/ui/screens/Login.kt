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
import androidx.navigation.NavController

import com.example.fraudlens.ui.navigation.Screen

import com.example.fraudlens.ui.theme.*
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
//import com.example.fraudlens.viewmodel.PaymentViewModel

//@Preview
@Composable
fun SignIn(
    navController: NavController,
    viewModel: FirestorePaymentViewModel

) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isChecked by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(30.dp)
    ) {
        Column {
            Text(
                text = "Hello",
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Again!",
                color = colorScheme.primary,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Welcome back you’ve been missed",
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            val textFieldColors = TextFieldDefaults.colors(
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

            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Enter registered email", color = colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.clickable(){
                            email = ""
                        }
                    )
                },
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(15.dp))

            TextField(
                value = password,
                onValueChange = { password = it },
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

            Spacer(Modifier.height(5.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = colorScheme.primary,
                            uncheckedColor = colorScheme.onSurfaceVariant
                        )
                    )
                    Text("Remember me", color = colorScheme.onSurface)
                }

                Text(
                    text = "Forgot the password",
                    color = colorScheme.primary,
                    modifier = Modifier.clickable { /* TODO */ }
                )
            }

            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()){
                    viewModel.checkLogin(context,email,password) { flag,result->
                        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                        if(flag) {
                            navController.navigate(Screen.biometricCheck.route)
                            }
                        }
                    }
                    else{
                        Toast.makeText(context, "Enter correct email and password", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = colorScheme.onPrimary,
                    containerColor = colorScheme.primary
                ),
            ) {
                Text("Login")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { /* Facebook */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1877F2), // Facebook Blue
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.mipmap.sym_def_app_icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Facebook")
                }

                Button(
                    onClick = { /* Google/Chrome */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.mipmap.sym_def_app_icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Chrome")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Don't have an account? Create one",
                    color = colorScheme.primary,
                    modifier = Modifier.clickable {
                        navController.navigate(Screen.signup.route)
                    }
                )
            }
        }
    }
}