package com.example.fraudlens.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
//import com.example.fraudlens.data.local.entities.DeviceInfo
import com.example.fraudlens.data.local.entities.FirestoreDeviceInfo
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


//fun getDeviceInfo(context: Context, userId: Long): DeviceInfo {
//    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
//    val deviceModel = Build.MODEL ?: "Unknown"
//    val osVersion = Build.VERSION.RELEASE ?: "Unknown"
//    val isRooted = checkIsDeviceRooted()
//
//    return DeviceInfo(
//        userId = userId,
//        deviceId = deviceId,
//        deviceModel = deviceModel,
//        osVersion = osVersion,
//        isRooted = isRooted
//    )
//}
fun getDeviceInfoFirestore(context: Context, userId: String): FirestoreDeviceInfo {
    val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    val deviceModel = Build.MODEL ?: "Unknown"
    val osVersion = Build.VERSION.RELEASE ?: "Unknown"
    val isRooted = checkIsDeviceRooted()

    return FirestoreDeviceInfo(
        userId = userId,
        deviceId = deviceId,
        deviceModel = deviceModel,
        osVersion = osVersion,
        isRooted = isRooted
    )
}

fun checkIsDeviceRooted(): Boolean {
    val buildTags = Build.TAGS
    val paths = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su"
    )
    return buildTags?.contains("test-keys") == true ||
            paths.any { File(it).exists() }
}

const val PROMPT = "You are a financial scam detection expert for Indian users. Your task is to analyze the following message and determine if it is a scam. Based on your knowledge base, provide your analysis in the JSON format specified below. The 'reasoning' field in your JSON output MUST be in the same language as the input message.\n\nKNOWLEDGE BASE:\n- Legitimate Messages: Sent from alphanumeric IDs (e.g., VM-HDFCBK), use partial account numbers, have a professional tone, and use official bank domains.\n- Scam Messages: Sent from mobile numbers, create urgency/fear (e.g., 'account blocked'), offer rewards (e.g., 'lottery win'), request sensitive info (PIN, OTP), or use suspicious links (URL shorteners, non-official domains, .apk files).\n\nJSON OUTPUT FORMAT:\n{\n  \"is_scam\": <A boolean value (true or false)>,\n  \"confidence_score\": <A float between 0.0 and 1.0>,\n  \"reasoning\": <A brief, clear explanation in the same language as the input message>,\n \"recommendation\": \"<Actionable advice for the user, e.g., 'Delete this message. Report the scammer.'>\"\n}"



