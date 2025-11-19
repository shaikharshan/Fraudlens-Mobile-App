package com.example.fraudlens.ui.screens

import com.example.fraudlens.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.fraudlens.viewmodel.GeminiUiState
import com.example.fraudlens.viewmodel.GeminiViewModel


// --- Data Structures and Localizations ---

// Represents the strings for a given language
data class LanguagePack(
    val languageName: String,
    val screenTitle: String,
    val senderLabel: String,
    val senderPlaceholder: String,
    val messageLabel: String,
    val messagePlaceholder: String,
    val checkButtonText: String,
    val resultTitle: String,
    val isScam: String,
    val notScam: String,
    val confidence: String,
    val reasoning: String,
    val recommendation: String,
    val loading: String
)

// In a real app, this would come from string resources (strings.xml)
object AppStrings {
    val languages = listOf("English", "हिन्दी", "मराठी", "বাংলা", "தமிழ்", "తెలుగు")

    val english = LanguagePack(
        languageName = "English",
        screenTitle = "Scam Message Checker",
        senderLabel = "Sender's Number or ID",
        senderPlaceholder = "e.g., VM-CANBNK",
        messageLabel = "Message Content",
        messagePlaceholder = "Paste the suspicious message here",
        checkButtonText = "Check for Scam",
        resultTitle = "Analysis Result",
        isScam = "Likely a Scam",
        notScam = "Likely Safe",
        confidence = "Confidence",
        reasoning = "Reasoning",
        recommendation = "Our Advice",
        loading = "Analyzing..."
    )

    val hindi = LanguagePack(
        languageName = "हिन्दी",
        screenTitle = "स्कैम संदेश चेकर",
        senderLabel = "प्रेषक का नंबर या आईडी",
        senderPlaceholder = "उदा., VM-CANBNK",
        messageLabel = "संदेश की सामग्री",
        messagePlaceholder = "संदिग्ध संदेश यहां पेस्ट करें",
        checkButtonText = "स्कैम की जाँच करें",
        resultTitle = "विश्लेषण परिणाम",
        isScam = "संभवतः एक स्कैम",
        notScam = "संभवतः सुरक्षित",
        confidence = "आत्मविश्वास",
        reasoning = "तर्क",
        recommendation = "हमारी सलाह",
        loading = "विश्लेषण हो रहा है..."
    )

    // Add other languages similarly...
    val marathi = LanguagePack(
        languageName = "मराठी",
        screenTitle = "स्कॅम मेसेज तपासक",
        senderLabel = "प्रेषकाचा नंबर किंवा आयडी",
        senderPlaceholder = "उदा., VM-CANBNK",
        messageLabel = "मेसेजमधील मजकूर",
        messagePlaceholder = "संशयास्पद मेसेज येथे पेस्ट करा",
        checkButtonText = "स्कॅम तपासा",
        resultTitle = "विश्लेषण परिणाम",
        isScam = "बहुधा स्कॅम आहे",
        notScam = "बहुधा सुरक्षित आहे",
        confidence = "आत्मविश्वास",
        reasoning = "तर्क",
        recommendation = "आमचा सल्ला",
        loading = "विश्लेषण होत आहे..."
    )

    val bengali = LanguagePack(
        languageName = "বাংলা",
        screenTitle = "স্ক্যাম বার্তা পরীক্ষক",
        senderLabel = "প্রেরকের নম্বর বা আইডি",
        senderPlaceholder = "যেমন, VM-CANBNK",
        messageLabel = "বার্তার বিষয়বস্তু",
        messagePlaceholder = "সন্দেহজনক বার্তাটি এখানে পেস্ট করুন",
        checkButtonText = "স্ক্যামের জন্য পরীক্ষা করুন",
        resultTitle = "বিশ্লেষণ ফলাফল",
        isScam = "সম্ভবত একটি স্ক্যাম",
        notScam = "সম্ভবত নিরাপদ",
        confidence = "আত্মবিশ্বাস",
        reasoning = "যুক্তি",
        recommendation = "আমাদের পরামর্শ",
        loading = "বিশ্লেষণ করা হচ্ছে..."
    )

    val tamil = LanguagePack(
        languageName = "தமிழ்",
        screenTitle = "மோசடி செய்தி சரிபார்ப்பவர்",
        senderLabel = "அனுப்புநரின் எண் அல்லது ஐடி",
        senderPlaceholder = "எ.கா., VM-CANBNK",
        messageLabel = "செய்தியின் உள்ளடக்கம்",
        messagePlaceholder = "சந்தேகத்திற்கிடமான செய்தியை இங்கே ஒட்டவும்",
        checkButtonText = "மோசடியைச் சரிபார்க்கவும்",
        resultTitle = "பகுப்பாய்வு முடிவு",
        isScam = "மோசடியாக இருக்கலாம்",
        notScam = "பாதுகாப்பாக இருக்கலாம்",
        confidence = "நம்பிக்கை",
        reasoning = "காரணம்",
        recommendation = "எங்கள் பரிந்துரை",
        loading = "பகுப்பாய்வு செய்யப்படுகிறது..."
    )

    val telugu = LanguagePack(
        languageName = "తెలుగు",
        screenTitle = "స్కామ్ సందేశ తనిఖీ",
        senderLabel = "పంపినవారి నంబర్ లేదా ఐడి",
        senderPlaceholder = "ఉదా., VM-CANBNK",
        messageLabel = "సందేశం యొక్క కంటెంట్",
        messagePlaceholder = "అనుమానాస్పద సందేశాన్ని ఇక్కడ అతికించండి",
        checkButtonText = "స్కామ్ కోసం తనిఖీ చేయండి",
        resultTitle = "విశ్లేషణ ఫలితం",
        isScam = "బహుశా స్కామ్",
        notScam = "బహుశా సురక్షితం",
        confidence = "విశ్వాసం",
        reasoning = "కారణం",
        recommendation = "మా సిఫార్సు",
        loading = "విశ్లేషిస్తోంది..."
    )

    fun getPack(language: String): LanguagePack {
        return when (language) {
            "हिन्दी" -> hindi
            "मराठी" -> marathi
            "বাংলা" -> bengali
            "தமிழ்" -> tamil
            "తెలుగు" -> telugu
            else -> english
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScamCheckerScreen(
    navController: NavController,
    viewModel: GeminiViewModel = hiltViewModel()
) {


    var selectedLanguage by remember { mutableStateOf(AppStrings.languages[0]) }
    val strings = remember(selectedLanguage) { AppStrings.getPack(selectedLanguage) }

    // State for input fields
    var senderInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }

    // UI State from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.screenTitle, fontWeight = FontWeight.Bold) },
                actions = { LanguageSelector(selectedLanguage) { selectedLanguage = it } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = Color(0xFFF0F4F8)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Input Section
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Sender Input Field
                    OutlinedTextField(
                        value = senderInput,
                        onValueChange = { senderInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(strings.senderLabel) },
                        placeholder = { Text(strings.senderPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = "Sender") },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                        trailingIcon = {
                            Icon(Icons.Default.Clear, contentDescription = "clear",
                                Modifier.clickable(){
                                    senderInput = ""
                                })

                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Message Input Field
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        label = { Text(strings.messageLabel) },
                        placeholder = { Text(strings.messagePlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = "Message") },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                        trailingIcon = {
                            Icon(Icons.Default.Clear, contentDescription = "clear",
                                Modifier.clickable(){
                                    messageInput = ""
                                })
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    val combinedText = "Sender: ${senderInput.trim()}\nMessage: ${messageInput.trim()}"
                    if (messageInput.isNotBlank()) {
                        viewModel.invoke(combinedText)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(50)),
                shape = RoundedCornerShape(50),
                enabled = messageInput.isNotBlank() && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(strings.checkButtonText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Result Display Section
            AnimatedVisibility(
                visible = uiState.isLoading || uiState.errorMessage != null || uiState.analysisResult != null || uiState.rawJsonBlob != null,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                ResultCard(uiState = uiState, strings = strings)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(id=R.drawable.outline_language_chinese_wubi_24),
                contentDescription = "Select Language",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppStrings.languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang) },
                    onClick = {
                        onLanguageSelected(lang)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ResultCard(uiState: GeminiUiState, strings: LanguagePack) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.resultTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(strings.loading, style = MaterialTheme.typography.bodyLarge)
                }
                uiState.errorMessage != null -> {
                    InfoRow(
                        icon = Icons.Default.Clear,
                        iconColor = MaterialTheme.colorScheme.error,
                        title = "Error",
                        content = uiState.errorMessage
                    )
                }
                uiState.rawJsonBlob != null -> {
                    InfoRow(
                        icon = Icons.Default.Clear,
                        iconColor = Color.DarkGray,
                        title = "Response",
                        content = uiState.rawJsonBlob
                    )
                }
                uiState.analysisResult != null -> {
                    val result = uiState.analysisResult
                    val isScam = result.is_scam
                    val resultColor = if (isScam) Color(0xFFD32F2F) else Color(0xFF388E3C)
                    val resultIcon = if (isScam) Icons.Default.Clear else Icons.Default.CheckCircle
                    val resultText = if (isScam) strings.isScam else strings.notScam

                    // Main Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = resultIcon,
                            contentDescription = "Status",
                            tint = resultColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = resultText,
                            color = resultColor,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // Details
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        InfoRow(title = strings.confidence, content = "${(result.confidence_score * 100).toInt()}%")
                        InfoRow(title = strings.reasoning, content = result.reasoning)
                        InfoRow(title = strings.recommendation, content = result.recommendation, isRecommendation = true)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    title: String,
    content: String,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    isRecommendation: Boolean = false
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = if(isRecommendation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = if (isRecommendation) Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(8.dp) else Modifier
        )
    }
}

