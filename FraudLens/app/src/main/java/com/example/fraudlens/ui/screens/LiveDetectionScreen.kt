package com.example.fraudlens.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fraudlens.viewmodel.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LiveDetectionScreen(
    navController: NavController,
    viewModel: LiveDetectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showSettings by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("YOUR_KEY") }

    // Audio permission handling
    val audioPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    ) { granted ->
        if (granted) {
            viewModel.checkAudioPermission()
        }
    }

    LaunchedEffect(Unit) {
        if (audioPermissionState.status.isGranted) {
            viewModel.checkAudioPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FraudLens Live", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(
                            containerColor = Color(0xFF4CAF50)
                        ) {
                            Text("Optimized", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                actions = {
                    // API Call Counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "API: ${uiState.apiCallCount}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Connection status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.isConnected) Icons.Default.Check else Icons.Default.Clear,
                            contentDescription = "Connection status",
                            tint = if (uiState.isConnected) Color(0xFF4CAF50) else Color(0xFFE57373),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (uiState.connectionStatus) {
                                ConnectionStatus.CONNECTED -> "Active"
                                ConnectionStatus.CONNECTING -> "Starting"
                                ConnectionStatus.ERROR -> "Error"
                                ConnectionStatus.DISCONNECTED -> "Offline"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),
                            Color(0xFF1565C0),
                            Color(0xFF1976D2)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                // Settings Panel
                AnimatedVisibility(
                    visible = showSettings,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SettingsPanelUpdated(
                        apiKey = apiKeyInput,
                        onApiKeyChange = { apiKeyInput = it },
                        selectedBatchMode = uiState.batchMode,
                        onBatchModeChange = { viewModel.setBatchMode(it) },
                        onSave = {
                            viewModel.saveApiKey(apiKeyInput)
                            showSettings = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Control Panel
                MainControlPanel(
                    uiState = uiState,
                    onStartMonitoring = {
                        if (!audioPermissionState.status.isGranted) {
                            audioPermissionState.launchPermissionRequest()
                        } else {
                            viewModel.startMonitoring(apiKeyInput)
                        }
                    },
                    onStopMonitoring = { viewModel.stopMonitoring() },
                    hasApiKey = apiKeyInput.isNotBlank(),
                    onTestCapture = { viewModel.triggerTestCapture() },
                    onAlwaysCaptureToggle = { enabled ->
                        if (enabled) {
                            viewModel.enableAlwaysCaptureMode()
                        } else {
                            viewModel.disableAlwaysCaptureMode()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error Message
                uiState.errorMessage?.let { error ->
                    ErrorCard(
                        message = error,
                        onDismiss = { viewModel.clearError() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Fraud Alerts
                if (uiState.fraudAlerts.isNotEmpty()) {
                    FraudAlertsSection(alerts = uiState.fraudAlerts)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Live Transcript
                if (uiState.transcript.isNotEmpty()) {
                    TranscriptSection(
                        transcript = uiState.transcript,
                        onClear = { viewModel.clearTranscript() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Instructions
                InstructionsSectionUpdated(currentMode = uiState.batchMode)

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SettingsPanelUpdated(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    selectedBatchMode: BatchMode,
    onBatchModeChange: (BatchMode) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Gemini API Key", color = Color.White.copy(0.8f)) },
                placeholder = { Text("Enter your API key", color = Color.White.copy(0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(0.5f)
                ),
                singleLine = true
            )

            Text(
                "Get free API key at: aistudio.google.com/app/apikey",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Batch Mode (API Optimization)",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            BatchMode.values().forEach { mode ->
                BatchModeOption(
                    mode = mode,
                    isSelected = selectedBatchMode == mode,
                    onSelect = { onBatchModeChange(mode) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                "💡 Aggressive mode recommended for free tier (5 RPM limit)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFC107),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Save Settings")
            }
        }
    }
}

@Composable
fun BatchModeOption(
    mode: BatchMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                Color.White.copy(0.2f)
            else
                Color.White.copy(0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.White,
                    unselectedColor = Color.White.copy(0.5f)
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    mode.description,
                    color = Color.White.copy(0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (mode == BatchMode.AGGRESSIVE) {
                Badge(
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text("Free Tier", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun MainControlPanel(
    uiState: LiveDetectionUiState,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    hasApiKey: Boolean,
    onTestCapture: () -> Unit,
    onAlwaysCaptureToggle: (Boolean) -> Unit
) {
    var alwaysCaptureEnabled by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (uiState.connectionStatus) {
                                ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionStatus.CONNECTING -> Color(0xFFFFC107)
                                else -> Color(0xFFE57373)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Status: ${
                        when (uiState.connectionStatus) {
                            ConnectionStatus.CONNECTED -> "Monitoring"
                            ConnectionStatus.CONNECTING -> "Connecting..."
                            ConnectionStatus.ERROR -> "Error"
                            ConnectionStatus.DISCONNECTED -> "Stopped"
                        }
                    }",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                uiState.lastActivity?.let {
                    Text(
                        text = "Last: $it",
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Level Indicator
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Audio Level",
                    color = Color.White.copy(0.7f),
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(uiState.audioLevel.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF4CAF50),
                                        Color(0xFFFFC107)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Control Button
            if (!uiState.isRecording) {
                Button(
                    onClick = onStartMonitoring,
                    enabled = hasApiKey,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasApiKey) Color(0xFF4CAF50) else Color.Gray
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Monitoring", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onStopMonitoring,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE57373)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Monitoring", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (!hasApiKey) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Please set your API key in settings",
                    color = Color(0xFFFFC107),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Test capture button (when recording)
            if (uiState.isRecording) {
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onTestCapture,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Test: Force 5s Capture")
                }

                Text(
                    "Bypasses speech detection for testing",
                    color = Color.White.copy(0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Always Capture Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alwaysCaptureEnabled)
                            Color(0xFFE57373).copy(0.3f)
                        else
                            Color.White.copy(0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Always Capture Mode",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                if (alwaysCaptureEnabled) "Processing audio every 3s" else "Use if speech detection fails",
                                color = Color.White.copy(0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Switch(
                            checked = alwaysCaptureEnabled,
                            onCheckedChange = { enabled ->
                                alwaysCaptureEnabled = enabled
                                onAlwaysCaptureToggle(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFE57373),
                                uncheckedThumbColor = Color.White.copy(0.6f),
                                uncheckedTrackColor = Color.White.copy(0.2f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE57373).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun FraudAlertsSection(alerts: List<FraudAlert>) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE57373),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "FRAUD ALERTS (${alerts.size})",
                color = Color(0xFFE57373),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        alerts.forEach { alert ->
            FraudAlertCard(alert)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FraudAlertCard(alert: FraudAlert) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD32F2F).copy(alpha = alpha * 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFCDD2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (alert.type == AlertType.AI_DETECTION) "AI DETECTED SCAM" else "PATTERN ALERT",
                        color = Color(0xFFFFCDD2),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    alert.timestamp,
                    color = Color(0xFFFFCDD2).copy(0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.White.copy(0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Confidence: ${(alert.confidence * 100).toInt()}%",
                color = if (alert.confidence > 0.8f) Color(0xFFFFCDD2) else Color(0xFFFFF59D),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Reasoning: ${alert.reasoning}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Recommendation: ${alert.recommendation}",
                color = Color(0xFFFFF59D),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.3f))
                    .padding(8.dp)
            ) {
                Text(
                    "Detected: ${alert.detectedText}",
                    color = Color.White.copy(0.8f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun TranscriptSection(transcript: String, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Live Transcript",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                TextButton(onClick = onClear) {
                    Text("Clear", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.3f))
                    .padding(12.dp)
            ) {
                Text(
                    transcript,
                    color = Color.White.copy(0.9f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun InstructionsSectionUpdated(currentMode: BatchMode) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "How to Use",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val instructions = listOf(
                "Enter your Gemini API key in settings",
                "Select batch mode (${currentMode.name} - ${currentMode.description})",
                "Allow audio recording permission when prompted",
                "Click \"Start Monitoring\" to begin",
                "The app batches audio to minimize API calls",
                "Fraud alerts appear when suspicious speech is detected",
                "For call monitoring, use speakerphone mode",
                "Use 'Test' button or 'Always Capture' if detection fails"
            )

            instructions.forEachIndexed { index, instruction ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        "${index + 1}.",
                        color = Color.White.copy(0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        instruction,
                        color = Color.White.copy(0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}