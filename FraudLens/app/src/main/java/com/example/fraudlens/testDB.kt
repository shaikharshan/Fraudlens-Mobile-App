//package com.example.fraudlens
//
//import android.util.Log
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.platform.LocalContext
//
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material3.Button
//import androidx.compose.material3.Divider
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Text
//
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.fraudlens.data.local.entities.FirestoreDeviceInfo
//import com.example.fraudlens.data.local.entities.FirestoreIPLog
//import com.example.fraudlens.data.local.entities.FirestoreLocationLog
//import com.example.fraudlens.data.local.entities.FirestoreTransactions
//import com.example.fraudlens.data.local.entities.FirestoreUser
//import com.example.fraudlens.data.local.entities.TransactionResponse
//
//
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import java.text.SimpleDateFormat
//import java.util.*
//import kotlin.math.*
//import kotlin.random.Random
//
//class FirestoreDataPopulator(private val firestore: FirebaseFirestore) {
//
//    // Enhanced Indian cities with tier-based distribution for more realistic patterns
//    private val tierOneCities = listOf(
//        Triple("Mumbai", 19.0760, 72.8777),
//        Triple("Delhi", 28.7041, 77.1025),
//        Triple("Bangalore", 12.9716, 77.5946),
//        Triple("Hyderabad", 17.3850, 78.4867),
//        Triple("Chennai", 13.0827, 80.2707),
//        Triple("Kolkata", 22.5726, 88.3639),
//        Triple("Pune", 18.5204, 73.8567),
//        Triple("Ahmedabad", 23.0225, 72.5714)
//    )
//
//    private val tierTwoCities = listOf(
//        Triple("Jaipur", 26.9124, 75.7873),
//        Triple("Surat", 21.1702, 72.8311),
//        Triple("Lucknow", 26.8467, 80.9462),
//        Triple("Kanpur", 26.4499, 80.3319),
//        Triple("Nagpur", 21.1458, 79.0882),
//        Triple("Indore", 22.7196, 75.8577),
//        Triple("Bhopal", 23.2599, 77.4126),
//        Triple("Coimbatore", 11.0168, 76.9558),
//        Triple("Vadodara", 22.3072, 73.1812),
//        Triple("Kochi", 9.9312, 76.2673)
//    )
//
//    private val tierThreeCities = listOf(
//        Triple("Mysore", 12.2958, 76.6394),
//        Triple("Nashik", 19.9975, 73.7898),
//        Triple("Rajkot", 22.3039, 70.8022),
//        Triple("Jodhpur", 26.2389, 73.0243),
//        Triple("Madurai", 9.9252, 78.1198),
//        Triple("Guwahati", 26.1445, 91.7362),
//        Triple("Chandigarh", 30.7333, 76.7794),
//        Triple("Thiruvananthapuram", 8.5241, 76.9366)
//    )
//
//    // Multinational fraud hotspot cities
//    private val fraudHotspotCities = listOf(
//        Triple("Lagos", 6.5244, 3.3792), // Nigeria
//        Triple("Moscow", 55.7558, 37.6176), // Russia
//        Triple("Beijing", 39.9042, 116.4074), // China
//        Triple("Bucharest", 44.4268, 26.1025), // Romania
//        Triple("Kiev", 50.4501, 30.5234), // Ukraine
//        Triple("Dhaka", 23.8103, 90.4125), // Bangladesh
//        Triple("Jakarta", -6.2088, 106.8456), // Indonesia
//        Triple("Manila", 14.5995, 120.9842), // Philippines
//        Triple("Istanbul", 41.0082, 28.9784), // Turkey
//        Triple("Cairo", 30.0444, 31.2357) // Egypt
//    )
//
//    // Enhanced bank data with realistic distribution
//    private val majorBanks = listOf(
//        "SBIN" to "State Bank of India",
//        "HDFC" to "HDFC Bank",
//        "ICIC" to "ICICI Bank",
//        "AXIS" to "Axis Bank"
//    )
//
//    private val privateBanks = listOf(
//        "KKBK" to "Kotak Mahindra Bank",
//        "YESB" to "Yes Bank",
//        "INDB" to "IndusInd Bank",
//        "FDRL" to "Federal Bank"
//    )
//
//    private val smallerBanks = listOf(
//        "KARB" to "Karnataka Bank",
//        "CNRB" to "Canara Bank",
//        "PUNB" to "Punjab National Bank",
//        "UBIN" to "Union Bank of India"
//    )
//
//    // Android versions
//    private val androidVersions = listOf("11", "12", "13", "14", "15")
//
//    // Enhanced device models with market share simulation
//    private val premiumDevices = listOf(
//        "iPhone 15 Pro", "iPhone 14 Pro", "Samsung Galaxy S24 Ultra",
//        "iPhone 13 Pro", "Samsung Galaxy S23 Ultra", "Google Pixel 8 Pro"
//    )
//
//    private val midRangeDevices = listOf(
//        "Samsung Galaxy A54", "OnePlus 11R", "Xiaomi 13",
//        "iPhone 13", "Samsung Galaxy A34", "Realme GT Neo 5",
//        "Vivo V29", "Oppo Reno 10", "Nothing Phone 2"
//    )
//
//    private val budgetDevices = listOf(
//        "Redmi Note 13", "Samsung Galaxy M34", "Realme C55",
//        "Moto G73", "Poco X5", "Infinix Note 30", "Tecno Spark 10"
//    )
//
//    // Enhanced suspicious IP ranges with more countries
//    private val highRiskIPs = listOf(
//        "185.220.100.240" to "Russia",
//        "198.51.100.45" to "Nigeria",
//        "203.0.113.78" to "China",
//        "192.0.2.156" to "Unknown",
//        "198.51.100.89" to "Romania",
//        "41.77.136.250" to "Nigeria",
//        "91.207.175.104" to "Ukraine",
//        "103.243.24.98" to "Bangladesh",
//        "125.64.94.200" to "North Korea",
//        "196.216.2.75" to "Ghana"
//    )
//
//    private val mediumRiskIPs = listOf(
//        "190.2.153.67" to "Brazil",
//        "177.87.45.123" to "Argentina",
//        "201.23.67.89" to "Mexico",
//        "85.195.234.78" to "Turkey",
//        "213.108.156.45" to "Poland"
//    )
//
//    // Indian ISP-based IPs for realistic distribution
//    private val indianISPs = mapOf(
//        "Airtel" to listOf("106.51.25.123", "117.239.67.45", "150.129.45.78"),
//        "Jio" to listOf("152.58.45.67", "203.192.225.156", "14.139.56.89"),
//        "VI" to listOf("157.32.123.45", "49.207.45.123", "223.233.67.89"),
//        "BSNL" to listOf("121.244.123.67", "59.144.78.123", "202.83.21.45")
//    )
//
//    // Fraud pattern generators
//    private val fraudPatterns = listOf(
//        "rapid_succession", "location_jumping", "round_amount",
//        "high_value", "unusual_time", "new_device", "suspicious_ip"
//    )
//
//    suspend fun populateDatabase() {
//        println("Starting enhanced database population...")
//
//        // Create users with realistic distribution
//        val users = createUsers(50)
//
//        // Create devices with market share simulation
//        createDevices(users)
//
//        // Create transactions with enhanced fraud patterns
//        createTransactions(users, 200)
//
//        // Create specific fraud scenarios
//        createFraudScenarios(users, 30)
//
//        // Create more pending transaction scenarios
//        createPendingTransactionScenarios(users, 25)
//
//        println("Enhanced database population completed!")
//    }
//
//    private suspend fun createUsers(count: Int): List<FirestoreUser> {
//        val users = mutableListOf<FirestoreUser>()
//        val firstNames = listOf(
//            "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Krishna", "Ishaan", "Shaurya",
//            "Aadhya", "Ananya", "Diya", "Saanvi", "Kavya", "Priya", "Ishika", "Anika", "Riya", "Myra",
//            "Raj", "Amit", "Rohit", "Vikram", "Sanjay", "Karthik", "Rahul", "Arun", "Suresh", "Mahesh",
//            "Sneha", "Anjali", "Deepika", "Meera", "Pooja", "Divya", "Nisha", "Lakshmi", "Sunita", "Kavita"
//        )
//
//        val lastNames = listOf(
//            "Sharma", "Patel", "Kumar", "Singh", "Gupta", "Agarwal", "Jain", "Reddy", "Nair", "Iyer",
//            "Bansal", "Malhotra", "Chopra", "Verma", "Shah", "Mehta", "Yadav", "Mishra", "Pandey", "Joshi"
//        )
//
//        for (i in 1..count) {
//            val firstName = firstNames.random()
//            val lastName = lastNames.random()
//
//            // Realistic email domains
//            val emailDomains = listOf("gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "rediffmail.com")
//            val email = "${firstName.lowercase()}.${lastName.lowercase()}${Random.nextInt(100, 9999)}@${emailDomains.random()}"
//
//            // More realistic phone numbers
//            val phonePrefix = listOf("9", "8", "7", "6").random()
//            val phone = "$phonePrefix${Random.nextInt(100000000, 999999999)}"
//
//            // Bank selection with realistic distribution
//            val bank = when (Random.nextInt(100)) {
//                in 0..49 -> majorBanks.random() // 50% major banks
//                in 50..79 -> privateBanks.random() // 30% private banks
//                else -> smallerBanks.random() // 20% smaller banks
//            }
//
//            val vpa = "${firstName.lowercase()}${Random.nextInt(100, 9999)}@${bank.first.lowercase()}"
//            val ifsc = "${bank.first}000${Random.nextInt(1000, 9999)}"
//
//            // More realistic balance distribution
//            val balance = when (Random.nextInt(100)) {
//                in 0..39 -> Random.nextDouble(500.0, 5000.0) // 40% low balance
//                in 40..79 -> Random.nextDouble(5000.0, 50000.0) // 40% medium balance
//                in 80..94 -> Random.nextDouble(50000.0, 200000.0) // 15% high balance
//                else -> Random.nextDouble(200000.0, 1000000.0) // 5% very high balance
//            }
//
//            val user = FirestoreUser(
//                userId = UUID.randomUUID().toString(),
//                username = "$firstName $lastName",
//                email = email,
//                password = "password123",
//                phone = phone,
//                registeredAt = System.currentTimeMillis() - Random.nextLong(0, 730L * 24 * 60 * 60 * 1000), // Up to 2 years
//                bankVPA = vpa,
//                bankIFSC = ifsc,
//                balance = balance,
//                biometricEnabled = Random.nextInt(100) < 75 // 75% have biometric enabled
//            )
//
//            users.add(user)
//            firestore.collection("users").document(user.userId).set(user).await()
//
//            if (i % 10 == 0) {
//                println("Created $i users...")
//            }
//        }
//
//        println("Created ${users.size} users with realistic distribution")
//        return users
//    }
//
//    private suspend fun createDevices(users: List<FirestoreUser>) {
//        for (user in users) {
//            // Device count based on user balance (wealth indicator)
//            val deviceCount = when {
//                user.balance > 100000 -> Random.nextInt(2, 4) // Wealthy users have more devices
//                user.balance > 20000 -> Random.nextInt(1, 3)
//                else -> 1 // Budget users typically have one device
//            }
//
//            for (i in 1..deviceCount) {
//                // Device selection based on user wealth
//                val deviceModel = when {
//                    user.balance > 100000 -> {
//                        when (Random.nextInt(100)) {
//                            in 0..69 -> premiumDevices.random() // 70% premium
//                            in 70..89 -> midRangeDevices.random() // 20% mid-range
//                            else -> budgetDevices.random() // 10% budget
//                        }
//                    }
//                    user.balance > 20000 -> {
//                        when (Random.nextInt(100)) {
//                            in 0..29 -> premiumDevices.random() // 30% premium
//                            in 30..79 -> midRangeDevices.random() // 50% mid-range
//                            else -> budgetDevices.random() // 20% budget
//                        }
//                    }
//                    else -> {
//                        when (Random.nextInt(100)) {
//                            in 0..9 -> premiumDevices.random() // 10% premium
//                            in 10..39 -> midRangeDevices.random() // 30% mid-range
//                            else -> budgetDevices.random() // 60% budget
//                        }
//                    }
//                }
//
//                val device = FirestoreDeviceInfo(
//                    userId = user.userId,
//                    deviceId = UUID.randomUUID().toString(),
//                    deviceModel = deviceModel,
//                    osVersion = androidVersions.random(),
//                    isRooted = Random.nextInt(1000) < 15, // 1.5% chance of rooted device
//                    lastActive = System.currentTimeMillis() - Random.nextLong(0, 30L * 24 * 60 * 60 * 1000)
//                )
//
//                firestore.collection("users")
//                    .document(user.userId)
//                    .collection("devices")
//                    .add(device)
//                    .await()
//            }
//        }
//
//        println("Created devices with realistic market distribution")
//    }
//
//    private suspend fun createTransactions(users: List<FirestoreUser>, count: Int) {
//        for (i in 1..count) {
//            val payer = users.random()
//            val receiver = users.filter { it.userId != payer.userId }.random()
//
//            // More realistic amount distribution
//            val amount = when (Random.nextInt(100)) {
//                in 0..49 -> Random.nextDouble(50.0, 500.0) // 50% small amounts
//                in 50..79 -> Random.nextDouble(500.0, 5000.0) // 30% medium amounts
//                in 80..94 -> Random.nextDouble(5000.0, 25000.0) // 15% large amounts
//                else -> Random.nextDouble(25000.0, 100000.0) // 5% very large amounts
//            }
//
//            // Enhanced location selection with realistic distribution
//            val (locationLog, city) = createLocationLog(payer)
//            val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//            // Enhanced IP log creation
//            val (ipLog, isHighRisk) = createIPLog(city)
//            val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//            // Enhanced fraud scoring with multiple factors (FIXED: Convert to decimal)
//            val fraudScore = calculateFraudScore(amount, locationLog, ipLog, payer)
//            val modelDecision = fraudScore > 0.7f || locationLog.isSuspicious || ipLog.isBlocked
//
//            val status = when {
//                modelDecision -> TransactionResponse.BLOCKED.value
//                fraudScore > 0.5f -> if (Random.nextInt(100) < 25) TransactionResponse.PENDING.value else TransactionResponse.APPROVED.value // Increased pending chance
//                fraudScore > 0.3f -> if (Random.nextInt(100) < 15) TransactionResponse.PENDING.value else TransactionResponse.APPROVED.value // Medium risk pending
//                Random.nextInt(100) < 3 -> TransactionResponse.PENDING.value // Random pending cases
//                else -> TransactionResponse.APPROVED.value
//            }
//
//            val transaction = FirestoreTransactions(
//                payerUserId = payer.userId,
//                payerDeviceId = UUID.randomUUID().toString(),
//                payerIFSC = payer.bankIFSC,
//                payerVpa = payer.bankVPA,
//                receiverUserId = receiver.userId,
//                receiverVpa = receiver.bankVPA,
//                receiverIfsc = receiver.bankIFSC,
//                amount = amount,
//                timestamp = generateRealisticTimestamp(),
//                status = status,
//                locationLogId = locationRef.id,
//                ipLogId = ipRef.id,
//                fraudScore = fraudScore, // Now returns 0.0-1.0
//                ipRiskScore = ipLog.riskScore / 100f, // Convert to decimal
//                locationRiskScore = if (locationLog.isSuspicious) Random.nextFloat() * 0.5f + 0.5f else Random.nextFloat() * 0.3f,
//                modelDecision = modelDecision
//            )
//
//            firestore.collection("transactions").add(transaction).await()
//
//            if (i % 20 == 0) {
//                println("Created $i transactions...")
//            }
//        }
//
//        println("Created $count transactions with enhanced patterns")
//    }
//
//    private fun createLocationLog(user: FirestoreUser): Pair<FirestoreLocationLog, Triple<String, Double, Double>> {
//        // 80% transactions from Indian cities, 20% from other locations (including fraud hotspots)
//        val city = when (Random.nextInt(100)) {
//            in 0..59 -> tierOneCities.random() // 60% tier 1 cities
//            in 60..79 -> tierTwoCities.random() // 20% tier 2 cities
//            in 80..89 -> tierThreeCities.random() // 10% tier 3 cities
//            else -> fraudHotspotCities.random() // 10% fraud hotspots
//        }
//
//        // Add realistic coordinate variation
//        val locationVariation = when {
//            fraudHotspotCities.contains(city) -> Random.nextDouble(0.5, 2.0) // Higher variation for fraud locations
//            else -> Random.nextDouble(0.01, 0.1) // Normal variation for legitimate locations
//        }
//
//        val latitude = city.second + Random.nextDouble(-locationVariation, locationVariation)
//        val longitude = city.third + Random.nextDouble(-locationVariation, locationVariation)
//
//        // Calculate realistic deviation patterns
//        val deviation = when {
//            fraudHotspotCities.contains(city) -> Random.nextDouble(1000.0, 15000.0) // International locations
//            tierOneCities.contains(city) -> Random.nextDouble(0.0, 50.0) // Local movement
//            else -> Random.nextDouble(50.0, 500.0) // Inter-city movement
//        }
//
//        val isSuspicious = deviation >= 500.0 || fraudHotspotCities.contains(city)
//
//        val locationLog = FirestoreLocationLog(
//            latitude = latitude,
//            longitude = longitude,
//            deviationFromLast = deviation,
//            isSuspicious = isSuspicious
//        )
//
//        return Pair(locationLog, city)
//    }
//
//    private fun createIPLog(city: Triple<String, Double, Double>): Pair<FirestoreIPLog, Boolean> {
//        val isHighRisk = fraudHotspotCities.contains(city) || Random.nextInt(100) < 5
//
//        val (ipAddress, country, isp) = when {
//            isHighRisk -> {
//                val (ip, country) = highRiskIPs.random()
//                Triple(ip, country, "Unknown ISP")
//            }
//            Random.nextInt(100) < 10 -> {
//                val (ip, country) = mediumRiskIPs.random()
//                Triple(ip, country, "Foreign ISP")
//            }
//            else -> {
//                val (isp, ips) = indianISPs.entries.random().toPair()
//                Triple(ips.random(), "India", isp)
//            }
//        }
//
//        val abuseScore = when {
//            isHighRisk -> Random.nextInt(75, 100)
//            country != "India" -> Random.nextInt(40, 75)
//            else -> Random.nextInt(0, 30)
//        }
//
//        val isBlocked = abuseScore >= 80
//
//        val ipLog = FirestoreIPLog(
//            ipAddress = ipAddress,
//            riskScore = abuseScore.toFloat(),
//            isBlocked = isBlocked,
//            country = country,
//            isp = isp
//        )
//
//        return Pair(ipLog, isHighRisk)
//    }
//
//    // FIXED: Calculate fraud score as decimal (0.0-1.0) instead of percentage
//    private fun calculateFraudScore(
//        amount: Double,
//        locationLog: FirestoreLocationLog,
//        ipLog: FirestoreIPLog,
//        user: FirestoreUser
//    ): Float {
//        var score = 0f
//
//        // Amount-based scoring (converted to decimal)
//        score += when {
//            amount > 50000 -> 0.30f
//            amount > 25000 -> 0.20f
//            amount > 10000 -> 0.10f
//            else -> 0f
//        }
//
//        // Location-based scoring (converted to decimal)
//        if (locationLog.isSuspicious) score += 0.25f
//        if (locationLog.deviationFromLast > 1000) score += 0.15f
//
//        // IP-based scoring (converted to decimal)
//        score += (ipLog.riskScore / 100f) * 0.4f
//
//        // User pattern scoring (converted to decimal)
//        if (amount > user.balance * 0.5) score += 0.15f // Large relative to balance
//
//        // Round amount patterns (common in fraud) (converted to decimal)
//        if (amount % 1000 == 0.0 && amount > 5000) score += 0.10f
//
//        return minOf(score, 1.0f) // Ensure max is 1.0
//    }
//
//    // NEW: Create specific pending transaction scenarios
//    private suspend fun createPendingTransactionScenarios(users: List<FirestoreUser>, count: Int) {
//        println("Creating pending transaction scenarios...")
//
//        for (i in 1..count) {
//            val pendingType = listOf(
//                "borderline_risk", "manual_review", "velocity_check",
//                "new_payee", "large_amount", "off_hours", "device_change"
//            ).random()
//
//            when (pendingType) {
//                "borderline_risk" -> createBorderlineRiskPending(users)
//                "manual_review" -> createManualReviewPending(users)
//                "velocity_check" -> createVelocityCheckPending(users)
//                "new_payee" -> createNewPayeePending(users)
//                "large_amount" -> createLargeAmountPending(users)
//                "off_hours" -> createOffHoursPending(users)
//                "device_change" -> createDeviceChangePending(users)
//            }
//        }
//
//        println("Created $count pending transaction scenarios")
//    }
//
//    private suspend fun createBorderlineRiskPending(users: List<FirestoreUser>) {
//        val payer = users.random()
//        val receiver = users.filter { it.userId != payer.userId }.random()
//        val amount = Random.nextDouble(8000.0, 15000.0) // Medium-high amount
//
//        // Mix of legitimate and slightly suspicious indicators
//        val city = if (Random.nextBoolean()) tierTwoCities.random() else tierOneCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.1, 0.1),
//            longitude = city.third + Random.nextDouble(-0.1, 0.1),
//            deviationFromLast = Random.nextDouble(200.0, 600.0), // Moderate deviation
//            isSuspicious = false
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (isp, ips) = indianISPs.entries.random().toPair()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ips.random(),
//            riskScore = Random.nextFloat() * 30 + 40, // 40-70 risk score
//            isBlocked = false,
//            country = "India",
//            isp = isp
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val fraudScore = Random.nextFloat() * 0.2f + 0.45f // 0.45-0.65 (borderline)
//
//        val transaction = FirestoreTransactions(
//            payerUserId = payer.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = payer.bankIFSC,
//            payerVpa = payer.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = fraudScore,
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.4f + 0.3f,
//            modelDecision = false
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createManualReviewPending(users: List<FirestoreUser>) {
//        val payer = users.random()
//        val receiver = users.filter { it.userId != payer.userId }.random()
//        val amount = Random.nextDouble(20000.0, 50000.0) // Large amount requiring review
//
//        val city = tierOneCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.05, 0.05),
//            longitude = city.third + Random.nextDouble(-0.05, 0.05),
//            deviationFromLast = Random.nextDouble(50.0, 200.0),
//            isSuspicious = false
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (isp, ips) = indianISPs.entries.random().toPair()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ips.random(),
//            riskScore = Random.nextFloat() * 25 + 15, // Low-medium risk
//            isBlocked = false,
//            country = "India",
//            isp = isp
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val fraudScore = Random.nextFloat() * 0.15f + 0.25f // 0.25-0.40 (low-medium risk)
//
//        val transaction = FirestoreTransactions(
//            payerUserId = payer.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = payer.bankIFSC,
//            payerVpa = payer.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = fraudScore,
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.3f + 0.1f,
//            modelDecision = false
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createVelocityCheckPending(users: List<FirestoreUser>) {
//        val payer = users.random()
//        val receiver = users.filter { it.userId != payer.userId }.random()
//        val amount = Random.nextDouble(5000.0, 12000.0)
//
//        val city = tierOneCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.02, 0.02),
//            longitude = city.third + Random.nextDouble(-0.02, 0.02),
//            deviationFromLast = Random.nextDouble(10.0, 100.0),
//            isSuspicious = false
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (isp, ips) = indianISPs.entries.random().toPair()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ips.random(),
//            riskScore = Random.nextFloat() * 20 + 10,
//            isBlocked = false,
//            country = "India",
//            isp = isp
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val fraudScore = Random.nextFloat() * 0.15f + 0.35f // 0.35-0.50 (velocity concern)
//
//        val transaction = FirestoreTransactions(
//            payerUserId = payer.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = payer.bankIFSC,
//            payerVpa = payer.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = fraudScore,
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.2f + 0.1f,
//            modelDecision = false
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createNewPayeePending(users: List<FirestoreUser>) {
//        val payer = users.random()
//        val receiver = users.filter { it.userId != payer.userId }.random()
//        val amount = Random.nextDouble(3000.0, 8000.0)
//
//        val city = tierOneCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.03, 0.03),
//            longitude = city.third + Random.nextDouble(-0.03, 0.03),
//            deviationFromLast = Random.nextDouble(20.0, 150.0),
//            isSuspicious = false
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (isp, ips) = indianISPs.entries.random().toPair()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ips.random(),
//            riskScore = Random.nextFloat() * 15 + 5,
//            isBlocked = false,
//            country = "India",
//            isp = isp
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val fraudScore = Random.nextFloat() * 0.20f + 0.30f // 0.30-0.50 (new payee risk)
//
//        val transaction = FirestoreTransactions(
//            payerUserId = payer.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = payer.bankIFSC,
//            payerVpa = payer.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = fraudScore,
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.25f + 0.15f,
//            modelDecision = false
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createLargeAmountPending(users: List<FirestoreUser>) {
//        val payer = users.filter { it.balance > 50000 }.randomOrNull() ?: users.random()
//        val receiver = users.filter { it.userId != payer.userId }.random()
//        val amount = Random.nextDouble(30000.0, 80000.0) // Large amount
//
//        val city = tierOneCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.02, 0.02),
//            longitude = city.third + Random.nextDouble(-0.02, 0.02),
//            deviationFromLast = Random.nextDouble(30.0, 120.0),
//            isSuspicious = false
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (isp, ips) = indianISPs.entries.random().toPair()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ips.random(),
//            riskScore = Random.nextFloat() * 20 + 8,
//            isBlocked = false,
//            country = "India",
//            isp = isp
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val fraudScore = Random.nextFloat() * 0.25f + 0.40f // 0.40-0.65 (large amount concern)
//
//        val transaction = FirestoreTransactions(
//            payerUserId = payer.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = payer.bankIFSC,
//            payerVpa = payer.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = fraudScore,
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.3f + 0.2f,
//            modelDecision = false
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createOffHoursPending(users: List<FirestoreUser>) {
//        val payer = users.random()
//        val receiver = users.filter { it.userId != payer.userId }.random()
//        val amount = Random.nextDouble(2000.0, 10000.0)
//
//        // Create transaction at unusual hours (12 AM - 5 AM or 11 PM - 12 AM)
//        val calendar = Calendar.getInstance()
//        val unusualHour = if (Random.nextBoolean()) {
//            Random.nextInt(0, 6) // 12 AM - 5 AM
//        } else {
//            23 // 11 PM
//        }
//        calendar.set(Calendar.HOUR_OF_DAY, unusualHour)
//        calendar.set(Calendar.MINUTE, Random.nextInt(0, 60))
//        calendar.add(Calendar.DAY_OF_MONTH, -Random.nextInt(1, 7))
//
//        val city = tierOneCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.04, 0.04),
//            longitude = city.third + Random.nextDouble(-0.04, 0.04),
//            deviationFromLast = Random.nextDouble(25.0, 180.0),
//            isSuspicious = false
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (isp, ips) = indianISPs.entries.random().toPair()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ips.random(),
//            riskScore = Random.nextFloat() * 25 + 12,
//            isBlocked = false,
//            country = "India",
//            isp = isp
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val fraudScore = Random.nextFloat() * 0.20f + 0.35f // 0.35-0.55 (unusual timing)
//
//        val transaction = FirestoreTransactions(
//            payerUserId = payer.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = payer.bankIFSC,
//            payerVpa = payer.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = Timestamp(calendar.time),
//            status = TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = fraudScore,
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.3f + 0.2f,
//            modelDecision = false
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createDeviceChangePending(users: List<FirestoreUser>) {
//        val payer = users.random()
//        val receiver = users.filter { it.userId != payer.userId }.random()
//        val amount = Random.nextDouble(4000.0, 12000.0)
//
//        // Create from a different device (but not suspicious)
//        val newDevice = FirestoreDeviceInfo(
//            userId = payer.userId,
//            deviceId = UUID.randomUUID().toString(),
//            deviceModel = midRangeDevices.random(),
//            osVersion = androidVersions.random(),
//            isRooted = false,
//            lastActive = System.currentTimeMillis()
//        )
//
//        firestore.collection("users")
//            .document(payer.userId)
//            .collection("devices")
//            .add(newDevice)
//            .await()
//
//        val city = tierOneCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.05, 0.05),
//            longitude = city.third + Random.nextDouble(-0.05, 0.05),
//            deviationFromLast = Random.nextDouble(40.0, 200.0),
//            isSuspicious = false
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (isp, ips) = indianISPs.entries.random().toPair()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ips.random(),
//            riskScore = Random.nextFloat() * 18 + 8,
//            isBlocked = false,
//            country = "India",
//            isp = isp
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val fraudScore = Random.nextFloat() * 0.20f + 0.32f // 0.32-0.52 (device change concern)
//
//        val transaction = FirestoreTransactions(
//            payerUserId = payer.userId,
//            payerDeviceId = newDevice.deviceId,
//            payerIFSC = payer.bankIFSC,
//            payerVpa = payer.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = fraudScore,
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.25f + 0.18f,
//            modelDecision = false
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createFraudScenarios(users: List<FirestoreUser>, count: Int) {
//        println("Creating specific fraud scenarios...")
//
//        for (i in 1..count) {
//            val pattern = fraudPatterns.random()
//            when (pattern) {
//                "rapid_succession" -> createRapidSuccessionFraud(users)
//                "location_jumping" -> createLocationJumpingFraud(users)
//                "round_amount" -> createRoundAmountFraud(users)
//                "high_value" -> createHighValueFraud(users)
//                "unusual_time" -> createUnusualTimeFraud(users)
//                "new_device" -> createNewDeviceFraud(users)
//                "suspicious_ip" -> createSuspiciousIPFraud(users)
//            }
//        }
//
//        println("Created $count fraud scenarios")
//    }
//
//    private suspend fun createRapidSuccessionFraud(users: List<FirestoreUser>) {
//        val fraudster = users.random()
//        val baseTime = System.currentTimeMillis()
//
//        // Create 5-8 transactions within 30 minutes
//        repeat(Random.nextInt(5, 9)) { i ->
//            val receiver = users.filter { it.userId != fraudster.userId }.random()
//            val amount = Random.nextDouble(2000.0, 8000.0)
//
//            val city = fraudHotspotCities.random()
//            val locationLog = FirestoreLocationLog(
//                latitude = city.second + Random.nextDouble(-0.1, 0.1),
//                longitude = city.third + Random.nextDouble(-0.1, 0.1),
//                deviationFromLast = if (i == 0) 0.0 else Random.nextDouble(5000.0, 12000.0),
//                isSuspicious = true
//            )
//
//            val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//            val (ip, country) = highRiskIPs.random()
//            val ipLog = FirestoreIPLog(
//                ipAddress = ip,
//                riskScore = Random.nextFloat() * 25 + 75,
//                isBlocked = true,
//                country = country,
//                isp = "Suspicious ISP"
//            )
//
//            val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//            val transaction = FirestoreTransactions(
//                payerUserId = fraudster.userId,
//                payerDeviceId = UUID.randomUUID().toString(),
//                payerIFSC = fraudster.bankIFSC,
//                payerVpa = fraudster.bankVPA,
//                receiverUserId = receiver.userId,
//                receiverVpa = receiver.bankVPA,
//                receiverIfsc = receiver.bankIFSC,
//                amount = amount,
//                timestamp = Timestamp(Date(baseTime + (i * Random.nextLong(60000, 1800000)))), // 1-30 min intervals
//                status = TransactionResponse.BLOCKED.value,
//                locationLogId = locationRef.id,
//                ipLogId = ipRef.id,
//                fraudScore = Random.nextFloat() * 0.2f + 0.8f, // 0.8-1.0
//                ipRiskScore = ipLog.riskScore / 100f,
//                locationRiskScore = Random.nextFloat() * 0.3f + 0.7f,
//                modelDecision = true
//            )
//
//            firestore.collection("transactions").add(transaction).await()
//        }
//    }
//
//    private suspend fun createLocationJumpingFraud(users: List<FirestoreUser>) {
//        val fraudster = users.random()
//        val baseTime = System.currentTimeMillis()
//
//        // Create transactions from different continents within hours
//        val locations = listOf(
//            Triple("Mumbai", 19.0760, 72.8777),
//            fraudHotspotCities.random(),
//            fraudHotspotCities.random()
//        )
//
//        locations.forEachIndexed { i, city ->
//            val receiver = users.filter { it.userId != fraudster.userId }.random()
//            val amount = Random.nextDouble(15000.0, 45000.0)
//
//            val locationLog = FirestoreLocationLog(
//                latitude = city.second + Random.nextDouble(-0.2, 0.2),
//                longitude = city.third + Random.nextDouble(-0.2, 0.2),
//                deviationFromLast = if (i == 0) 0.0 else Random.nextDouble(8000.0, 15000.0),
//                isSuspicious = true
//            )
//
//            val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//            val (ip, country) = if (i == 0) {
//                val randomISP = indianISPs.entries.random()
//                randomISP.value.random() to "India"
//            } else {
//                highRiskIPs.random()
//            }
//
//            val ipLog = FirestoreIPLog(
//                ipAddress = ip,
//                riskScore = if (i == 0) Random.nextFloat() * 30 else Random.nextFloat() * 25 + 75,
//                isBlocked = i > 0,
//                country = country,
//                isp = if (i == 0) indianISPs.keys.random() else "Foreign ISP"
//            )
//
//            val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//            val transaction = FirestoreTransactions(
//                payerUserId = fraudster.userId,
//                payerDeviceId = UUID.randomUUID().toString(),
//                payerIFSC = fraudster.bankIFSC,
//                payerVpa = fraudster.bankVPA,
//                receiverUserId = receiver.userId,
//                receiverVpa = receiver.bankVPA,
//                receiverIfsc = receiver.bankIFSC,
//                amount = amount,
//                timestamp = Timestamp(Date(baseTime + (i * Random.nextLong(3600000, 7200000)))), // 1-2 hour intervals
//                status = if (i == 0) TransactionResponse.APPROVED.value else TransactionResponse.BLOCKED.value,
//                locationLogId = locationRef.id,
//                ipLogId = ipRef.id,
//                fraudScore = if (i == 0) Random.nextFloat() * 0.4f else Random.nextFloat() * 0.2f + 0.8f, // 0.8-1.0 for fraud
//                ipRiskScore = ipLog.riskScore / 100f,
//                locationRiskScore = if (i == 0) Random.nextFloat() * 0.3f else Random.nextFloat() * 0.3f + 0.7f,
//                modelDecision = i > 0
//            )
//
//            firestore.collection("transactions").add(transaction).await()
//        }
//    }
//
//    private suspend fun createRoundAmountFraud(users: List<FirestoreUser>) {
//        val fraudster = users.random()
//        val receiver = users.filter { it.userId != fraudster.userId }.random()
//
//        // Suspicious round amounts
//        val suspiciousAmounts = listOf(10000.0, 25000.0, 50000.0, 75000.0, 100000.0)
//        val amount = suspiciousAmounts.random()
//
//        val city = fraudHotspotCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.3, 0.3),
//            longitude = city.third + Random.nextDouble(-0.3, 0.3),
//            deviationFromLast = Random.nextDouble(5000.0, 12000.0),
//            isSuspicious = true
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (ip, country) = highRiskIPs.random()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ip,
//            riskScore = Random.nextFloat() * 20 + 80,
//            isBlocked = true,
//            country = country,
//            isp = "Suspicious ISP"
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val transaction = FirestoreTransactions(
//            payerUserId = fraudster.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = fraudster.bankIFSC,
//            payerVpa = fraudster.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.BLOCKED.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = Random.nextFloat() * 0.15f + 0.85f, // 0.85-1.0 (very high fraud score)
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.3f + 0.7f,
//            modelDecision = true
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createHighValueFraud(users: List<FirestoreUser>) {
//        val fraudster = users.random()
//        val receiver = users.filter { it.userId != fraudster.userId }.random()
//
//        // Very high amounts that exceed typical user patterns
//        val amount = Random.nextDouble(100000.0, 500000.0)
//
//        val city = fraudHotspotCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.5, 0.5),
//            longitude = city.third + Random.nextDouble(-0.5, 0.5),
//            deviationFromLast = Random.nextDouble(8000.0, 15000.0),
//            isSuspicious = true
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (ip, country) = highRiskIPs.random()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ip,
//            riskScore = Random.nextFloat() * 15 + 85,
//            isBlocked = true,
//            country = country,
//            isp = "High Risk ISP"
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val transaction = FirestoreTransactions(
//            payerUserId = fraudster.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = fraudster.bankIFSC,
//            payerVpa = fraudster.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.BLOCKED.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = Random.nextFloat() * 0.1f + 0.9f, // 0.9-1.0 (maximum fraud score)
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.2f + 0.8f,
//            modelDecision = true
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createUnusualTimeFraud(users: List<FirestoreUser>) {
//        val fraudster = users.random()
//        val receiver = users.filter { it.userId != fraudster.userId }.random()
//        val amount = Random.nextDouble(5000.0, 20000.0)
//
//        // Create transaction at unusual hours (2-5 AM)
//        val calendar = Calendar.getInstance()
//        calendar.set(Calendar.HOUR_OF_DAY, Random.nextInt(2, 6))
//        calendar.set(Calendar.MINUTE, Random.nextInt(0, 60))
//        calendar.add(Calendar.DAY_OF_MONTH, -Random.nextInt(1, 30))
//
//        val city = fraudHotspotCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.2, 0.2),
//            longitude = city.third + Random.nextDouble(-0.2, 0.2),
//            deviationFromLast = Random.nextDouble(3000.0, 10000.0),
//            isSuspicious = true
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (ip, country) = highRiskIPs.random()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ip,
//            riskScore = Random.nextFloat() * 30 + 70,
//            isBlocked = Random.nextBoolean(),
//            country = country,
//            isp = "Night Activity ISP"
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val transaction = FirestoreTransactions(
//            payerUserId = fraudster.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = fraudster.bankIFSC,
//            payerVpa = fraudster.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = Timestamp(calendar.time),
//            status = if (ipLog.isBlocked) TransactionResponse.BLOCKED.value else TransactionResponse.PENDING.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = Random.nextFloat() * 0.25f + 0.65f, // 0.65-0.90 (high fraud score)
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.4f + 0.6f,
//            modelDecision = ipLog.isBlocked
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createNewDeviceFraud(users: List<FirestoreUser>) {
//        val fraudster = users.random()
//        val receiver = users.filter { it.userId != fraudster.userId }.random()
//        val amount = Random.nextDouble(8000.0, 30000.0)
//
//        // Create from a suspicious new device
//        val suspiciousDevice = FirestoreDeviceInfo(
//            userId = fraudster.userId,
//            deviceId = UUID.randomUUID().toString(),
//            deviceModel = "Unknown Device",
//            osVersion = "Modified OS",
//            isRooted = true,
//            lastActive = System.currentTimeMillis()
//        )
//
//        firestore.collection("users")
//            .document(fraudster.userId)
//            .collection("devices")
//            .add(suspiciousDevice)
//            .await()
//
//        val city = fraudHotspotCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.3, 0.3),
//            longitude = city.third + Random.nextDouble(-0.3, 0.3),
//            deviationFromLast = Random.nextDouble(6000.0, 12000.0),
//            isSuspicious = true
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val (ip, country) = highRiskIPs.random()
//        val ipLog = FirestoreIPLog(
//            ipAddress = ip,
//            riskScore = Random.nextFloat() * 25 + 75,
//            isBlocked = true,
//            country = country,
//            isp = "Rooted Device ISP"
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val transaction = FirestoreTransactions(
//            payerUserId = fraudster.userId,
//            payerDeviceId = suspiciousDevice.deviceId,
//            payerIFSC = fraudster.bankIFSC,
//            payerVpa = fraudster.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.BLOCKED.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = Random.nextFloat() * 0.2f + 0.8f, // 0.8-1.0 (high fraud score)
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.3f + 0.7f,
//            modelDecision = true
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    private suspend fun createSuspiciousIPFraud(users: List<FirestoreUser>) {
//        val fraudster = users.random()
//        val receiver = users.filter { it.userId != fraudster.userId }.random()
//        val amount = Random.nextDouble(3000.0, 15000.0)
//
//        // Use the most suspicious IP available
//        val (ip, country) = highRiskIPs.first() // Most suspicious IP
//
//        val city = fraudHotspotCities.random()
//        val locationLog = FirestoreLocationLog(
//            latitude = city.second + Random.nextDouble(-0.4, 0.4),
//            longitude = city.third + Random.nextDouble(-0.4, 0.4),
//            deviationFromLast = Random.nextDouble(7000.0, 14000.0),
//            isSuspicious = true
//        )
//
//        val locationRef = firestore.collection("location_logs").add(locationLog).await()
//
//        val ipLog = FirestoreIPLog(
//            ipAddress = ip,
//            riskScore = 95f, // Maximum risk score
//            isBlocked = true,
//            country = country,
//            isp = "Blacklisted ISP"
//        )
//
//        val ipRef = firestore.collection("ip_logs").add(ipLog).await()
//
//        val transaction = FirestoreTransactions(
//            payerUserId = fraudster.userId,
//            payerDeviceId = UUID.randomUUID().toString(),
//            payerIFSC = fraudster.bankIFSC,
//            payerVpa = fraudster.bankVPA,
//            receiverUserId = receiver.userId,
//            receiverVpa = receiver.bankVPA,
//            receiverIfsc = receiver.bankIFSC,
//            amount = amount,
//            timestamp = generateRealisticTimestamp(),
//            status = TransactionResponse.BLOCKED.value,
//            locationLogId = locationRef.id,
//            ipLogId = ipRef.id,
//            fraudScore = 0.95f, // Maximum fraud score (decimal)
//            ipRiskScore = ipLog.riskScore / 100f,
//            locationRiskScore = Random.nextFloat() * 0.25f + 0.75f,
//            modelDecision = true
//        )
//
//        firestore.collection("transactions").add(transaction).await()
//    }
//
//    // Generate realistic time patterns with better distribution
//    private fun generateRealisticTimestamp(): Timestamp {
//        val now = System.currentTimeMillis()
//        val daysAgo = when (Random.nextInt(100)) {
//            in 0..29 -> Random.nextInt(0, 3) // 30% recent (0-3 days)
//            in 30..59 -> Random.nextInt(3, 7) // 30% last week
//            in 60..79 -> Random.nextInt(7, 14) // 20% last 2 weeks
//            in 80..89 -> Random.nextInt(14, 30) // 10% last month
//            else -> Random.nextInt(30, 90) // 10% older
//        }
//
//        val baseTime = now - (daysAgo * 24 * 60 * 60 * 1000)
//
//        // More realistic hour distribution
//        val hour = when (Random.nextInt(100)) {
//            in 0..5 -> Random.nextInt(0, 6) // 5% night (12 AM - 6 AM)
//            in 6..15 -> Random.nextInt(6, 9) // 10% early morning (6 AM - 9 AM)
//            in 16..55 -> Random.nextInt(9, 18) // 40% business hours (9 AM - 6 PM)
//            in 56..80 -> Random.nextInt(18, 22) // 25% evening (6 PM - 10 PM)
//            else -> Random.nextInt(22, 24) // 20% late night (10 PM - 12 AM)
//        }
//
//        val minute = Random.nextInt(0, 60)
//        val second = Random.nextInt(0, 60)
//
//        val calendar = Calendar.getInstance()
//        calendar.timeInMillis = baseTime
//        calendar.set(Calendar.HOUR_OF_DAY, hour)
//        calendar.set(Calendar.MINUTE, minute)
//        calendar.set(Calendar.SECOND, second)
//
//        return Timestamp(calendar.time)
//    }
//}
//
//// Usage function to call from your activity/fragment
//suspend fun populateFirestoreData() {
//    val firestore = FirebaseFirestore.getInstance()
//    val populator = FirestoreDataPopulator(firestore)
//    populator.populateDatabase()
//}
//
//class DataPopulationHelper(private val activity: ComponentActivity) {
//    fun startPopulation() {
//        activity.lifecycleScope.launch {
//            try {
//                populateFirestoreData()
//                Toast.makeText(activity, "Enhanced database populated successfully!", Toast.LENGTH_LONG).show()
//            } catch (e: Exception) {
//                Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
//                Log.e("DataPopulation", "Error populating database", e)
//            }
//        }
//    }
//}
//
//@Composable
//fun populateDBOnce() {
//    val context = LocalContext.current
//    val activity = context as androidx.activity.ComponentActivity
//
//    LaunchedEffect(Unit) {
//        DataPopulationHelper(activity).startPopulation()
//    }
//
//    // Enhanced loading message
//    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//        Column(horizontalAlignment = Alignment.CenterHorizontally) {
//            Text("Populating Enhanced Firestore Database...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
//            Spacer(modifier = Modifier.height(8.dp))
//            Text("Creating realistic fraud patterns with decimal scores and pending cases", fontSize = 14.sp)
//        }
//    }
//}