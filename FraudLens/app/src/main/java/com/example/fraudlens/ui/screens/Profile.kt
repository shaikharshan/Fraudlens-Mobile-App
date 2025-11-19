package com.example.fraudlens.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fraudlens.R
import com.example.fraudlens.data.local.entities.FirestoreUser
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: FirestorePaymentViewModel,
    navController: NavController

) {
    var user = viewModel.loggedUser.collectAsStateWithLifecycle().value
    var isEditing by remember { mutableStateOf(false) }



    var name by remember { mutableStateOf(user?.username) }
    var password by remember { mutableStateOf(user?.password) }
    var vpa by remember { mutableStateOf(user?.bankVPA) }
//    var profileImageUri by remember { mutableStateOf(user.profileImageUri) }

//    val imageLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent()
//    ) { uri: Uri? ->
//        uri?.let {
//            profileImageUri = it.toString()
//        }
//    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Info") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Handle back navigation
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Profile Image
            Box(modifier = Modifier.padding(16.dp)) {
//                val painter = rememberAsyncImagePainter(
//                    ImageRequest.Builder(context)
//                        .data(profileImageUri.ifEmpty { R.drawable.default_avatar })
//                        .crossfade(true)
//                        .build()
//                )
                Image(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
//                        .clickable { imageLauncher.launch("image/*") }
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Icon",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(Color.White, CircleShape)
                        .padding(4.dp)
                )
            }

            // Personal Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isEditing) {
                        EditableField("Your name", name.toString()) { name = it }
                        EditableField("VPA", vpa.toString()) { vpa = it }
                        EditableField("Password", password.toString(), true) { password = it }
                    } else {
                        InfoRow("Your name", name.toString())
                        InfoRow("VPA", vpa.toString())
                        InfoRow("Password", "*".repeat(password?.length ?: 0))
                    }
                }
            }

            // Contact Info
            Card(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Phone number", user?.phone ?: "")
                    InfoRow("Email", user?.email ?: "")
                }
            }

            // Edit Button
            Button(
                onClick = {
                    if (isEditing) {
                        val updatedUser = mapOf<String,String>(
                            Pair("username" , name.toString()),
                            Pair("bankVPA" , vpa.toString()),
                            Pair("password" , password.toString()),
//                            profileImageUri = profileImageUri
                        )
                        viewModel.updateUser(updatedUser)
                        viewModel.setLoggedUser(user?.userId ?: "NA")
                    }
                    isEditing = !isEditing
                },
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isEditing) "Save" else "Edit")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EditableField(label: String, value: String, isPassword: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
    )
}
