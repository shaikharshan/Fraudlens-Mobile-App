package com.example.fraudlens.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.fraudlens.R
import com.example.fraudlens.data.local.entities.FirestoreTransactions
import com.example.fraudlens.data.local.entities.FirestoreUser
import com.example.fraudlens.data.local.entities.TransactionResponse
import com.example.fraudlens.ui.navigation.Screen
import com.example.fraudlens.viewmodel.FirestorePaymentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel



@Composable
fun ExitAppOnBackPressed() {
    val context = LocalContext.current
    var showToast by remember { mutableStateOf(false) }
    var backPressTime by remember { mutableStateOf(0L) }

    BackHandler(enabled = true) {
        if (backPressTime + 2000 > System.currentTimeMillis()) {
            (context as? Activity)?.finish()
        } else {
            showToast = true
            backPressTime = System.currentTimeMillis()
        }
    }

    LaunchedEffect(showToast) {
        if (showToast) {
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.delay(2000)
            showToast = false
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun Home(
    viewModel: FirestorePaymentViewModel = hiltViewModel(),
    navController: NavController
) {

    val user by viewModel.loggedUser.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    LaunchedEffect(user) {
        if (user != null) {
            viewModel.loadTransactions(user!!.userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_shield_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "FraudLens",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Column {
                        Text(
                            text = "Welcome, ${user?.username ?: "User"}",
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Balance Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = "Current Balance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "₹${String.format("%.2f", user?.balance ?: 0.0)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "UPI: ${user?.bankVPA ?: "Not Available"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                item {
                    TransactionStatisticsGraph(
                        transactions = transactions,
                        userId = user?.userId ?: ""
                    )
                }

                item {
                    Text(
                        text = "Recent Transactions",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val approvedTransactions = transactions.filter { it.status == TransactionResponse.APPROVED.value }
                if (approvedTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                            Text("No recent transactions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(approvedTransactions) { transaction ->
                        TransactionItem(transaction, user)
                    }
                }
            }
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BottomAppBarUnit("Home", Icons.Default.Home) {
                        navController.navigate(Screen.home.route) {
                            popUpTo(Screen.root.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    BottomAppBarUnit("Send", Icons.AutoMirrored.Filled.Send) {
                        navController.navigate(Screen.sendMoney.route) {
                            launchSingleTop = true
                        }
                    }
                    BottomAppBarUnit("Profile", Icons.Default.AccountCircle) {
                        navController.navigate(Screen.profile.route) {
                            launchSingleTop = true
                        }
                    }
                    BottomAppBarUnit("SMSCheck", Icons.Default.Search) {
                        navController.navigate(Screen.smsFraudCheck.route) {
                            launchSingleTop = true
                        }
                    }
                    BottomAppBarUnit("VishingCheck", Icons.Default.Call) {
                        navController.navigate(Screen.liveDetection.route) {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    )
    ExitAppOnBackPressed()
}

@Composable
fun TransactionStatisticsGraph(
    transactions: List<FirestoreTransactions>,
    userId: String
) {
    val dailyAmounts = remember(transactions, userId) {
        processDailyTransactions(transactions, userId)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "7-Day Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val percentageChange = calculatePercentageChange(dailyAmounts)
                        Icon(
                            imageVector = if (percentageChange >= 0) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (percentageChange >= 0) Color(0xFF388E3C) else Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${String.format("%.1f", abs(percentageChange))}% vs start of week",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
                Text(
                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TransactionBarChart(
                dailyAmounts = dailyAmounts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

// --- FIXED BAR CHART ---
@Composable
fun TransactionBarChart(
    dailyAmounts: List<DailyTransaction>,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<DailyTransaction?>(null) }
    // Ensure maxAmount is at least 1.0 to avoid division by zero.
    val maxAmount = remember(dailyAmounts) { (dailyAmounts.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1.0) }

    // For debugging: You can uncomment this log to see the data being fed to the chart
    // Log.d("ChartData", "MaxAmount: $maxAmount, DailyData: $dailyAmounts")

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Main chart area takes available vertical space
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bars Area
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom // This is crucial for bars to grow upwards
            ) {
                dailyAmounts.forEach { dayData ->
                    // Calculate the height fraction for the bar
                    val barHeightFraction by animateFloatAsState(
                        targetValue = (dayData.amount / maxAmount).toFloat(),
                        animationSpec = tween(durationMillis = 800),
                        label = "barHeight"
                    )

                    // Each bar is a Column with a Spacer and the visible bar Box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                selectedDay = if (selectedDay == dayData) null else dayData
                            },
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f) // Bar width is 70% of its column
                                .fillMaxHeight(barHeightFraction.coerceAtLeast(0.02f)) // Minimum height to be visible
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    color = if (selectedDay == dayData)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }

            // Y-axis labels on the right
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val maxLabel = if (maxAmount >= 1000) "₹${(maxAmount / 1000).toInt()}K" else "₹${maxAmount.toInt()}"
                Text(
                    text = maxLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "₹0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .padding(end = 40.dp), // Align with bars, accounting for Y-axis width
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dailyAmounts.forEach { dayData ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayData.dayOfWeekLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedDay == dayData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = dayData.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selectedDay == dayData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Tooltip replacement: Static info box for the selected day
        AnimatedVisibility(
            visible = selectedDay != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedDay?.let { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "On ${day.dayOfWeekLabel}, ${day.dateLabel}: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${String.format("%.2f", day.amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " in ${day.transactionCount} transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


data class DailyTransaction(
    val dayOfWeekLabel: String,
    val dateLabel: String,
    val amount: Double,
    val transactionCount: Int,
    val year: Int,
    val month: Int,
    val dayOfMonth: Int
)

fun processDailyTransactions(transactions: List<FirestoreTransactions>, userId: String): List<DailyTransaction> {
    val calendar = Calendar.getInstance()
    val dailyDataMap = linkedMapOf<String, DailyTransaction>() // Use LinkedHashMap to preserve insertion order

    // Initialize the last 7 days with zero values
    for (i in 6 downTo 0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -i)
        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val dayOfWeekLabel = if (i == 0) "Today" else SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
        val dateLabel = SimpleDateFormat("dd/MM", Locale.getDefault()).format(calendar.time)

        dailyDataMap[dayKey] = DailyTransaction(
            dayOfWeekLabel = dayOfWeekLabel,
            dateLabel = dateLabel,
            amount = 0.0,
            transactionCount = 0,
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH),
            dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Populate the map with actual transaction data
    transactions.forEach { transaction ->
        if (transaction.status == TransactionResponse.APPROVED.value) {
            val transactionDate = transaction.timestamp.toDate()
            val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(transactionDate)

            if (dailyDataMap.containsKey(dayKey)) {
                val existingData = dailyDataMap[dayKey]!!
                dailyDataMap[dayKey] = existingData.copy(
                    amount = existingData.amount + transaction.amount,
                    transactionCount = existingData.transactionCount + 1
                )
            }
        }
    }

    return dailyDataMap.values.toList()
}

fun calculatePercentageChange(dailyAmounts: List<DailyTransaction>): Double {
    if (dailyAmounts.size < 2) return 0.0

    val currentAmount = dailyAmounts.last().amount
    val previousAmount = dailyAmounts.first().amount

    return if (previousAmount > 0) {
        ((currentAmount - previousAmount) / previousAmount) * 100.0
    } else {
        if (currentAmount > 0) 100.0 else 0.0
    }
}

@Composable
fun TransactionItem(txn: FirestoreTransactions, user: FirestoreUser?) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isReceived = txn.receiverUserId == user?.userId
            Icon(
                imageVector = if (isReceived) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isReceived) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                    .padding(8.dp),
                tint = if (isReceived) Color(0xFF388E3C) else Color(0xFFD32F2F)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReceived) "From: ${txn.payerVpa}" else "To: ${txn.receiverVpa}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(txn.timestamp.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (isReceived) "+₹${String.format("%.2f", txn.amount)}" else "-₹${String.format("%.2f", txn.amount)}",
                color = if (isReceived) Color(0xFF388E3C) else Color(0xFFD32F2F),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun BottomAppBarUnit(text: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = text)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = text, fontSize = 12.sp)
    }
}