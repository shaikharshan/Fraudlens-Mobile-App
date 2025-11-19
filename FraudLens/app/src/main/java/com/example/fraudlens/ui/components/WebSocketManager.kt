package com.example.fraudlens.ui.components

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

sealed class WebSocketEvent {
    data class Connected(val message: String) : WebSocketEvent()
    data class TranscriptReceived(val text: String, val isUser: Boolean) : WebSocketEvent()
    data class FraudAnalysisReceived(val analysis: FraudAnalysis) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class AudioLevelUpdate(val level: Float) : WebSocketEvent()
}

data class FraudAnalysis(
    val is_scam: Boolean,
    val confidence_score: Float,
    val reasoning: String,
    val recommendation: String
)

@Singleton
class GeminiWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private var isSessionEstablished = false

    companion object {
        private const val TAG = "GeminiWebSocket"
        private const val FRAUD_DETECTION_PROMPT = """You are a financial scam detection expert for Indian users. SCENARIO: You are analyzing a video call/meet. Your task is to analyze the following message and determine if it is a scam. Based on your knowledge base, provide your analysis in the JSON format specified below. The 'reasoning' field in your JSON output MUST be in the same language as the input message.

KNOWLEDGE BASE:
- Legitimate Messages: Sent from alphanumeric IDs (e.g., VM-HDFCBK), use partial account numbers, have a professional tone, and use official bank domains.
- Scam Messages: Sent from mobile numbers, create urgency/fear (e.g., 'account blocked'), offer rewards (e.g., 'lottery win'), request sensitive info (PIN, OTP), or use suspicious links (URL shorteners, non-official domains, .apk files).

JSON OUTPUT FORMAT:
{
  "is_scam": <A boolean value (true or false)>,
  "confidence_score": <A float between 0.0 and 1.0>,
  "reasoning": <A brief, clear explanation in the same language as the input message>,
  "recommendation": "<Actionable advice for the user, e.g., 'Delete this message. Report the scammer.'>"
}"""
    }

    fun connect(apiKey: String): Flow<WebSocketEvent> = callbackFlow {
        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"

        Log.d(TAG, "Attempting to connect to WebSocket...")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully")
                isSessionEstablished = false

                // Send setup message
                val setupMessage = JsonObject().apply {
                    add("setup", JsonObject().apply {
                        addProperty("model", "models/gemini-2.0-flash-exp")
                        add("generationConfig", JsonObject().apply {
                            addProperty("temperature", 0.3)
                            addProperty("maxOutputTokens", 8192)
                            add("responseModalities", gson.toJsonTree(listOf("TEXT")))
                        })
                        add("systemInstruction", JsonObject().apply {
                            add("parts", gson.toJsonTree(listOf(
                                mapOf("text" to FRAUD_DETECTION_PROMPT)
                            )))
                        })
                        add("realtimeInputConfig", JsonObject().apply {
                            add("automaticActivityDetection", JsonObject().apply {
                                addProperty("disabled", false)
                                addProperty("startOfSpeechSensitivity", "START_SENSITIVITY_HIGH")
                                addProperty("endOfSpeechSensitivity", "END_SENSITIVITY_HIGH")
                                addProperty("prefixPaddingMs", 300)
                                addProperty("silenceDurationMs", 1000)
                            })
                            addProperty("activityHandling", "START_OF_ACTIVITY_INTERRUPTS")
                            addProperty("turnCoverage", "TURN_INCLUDES_ONLY_ACTIVITY")
                        })
                        add("inputAudioTranscription", JsonObject())
                        add("outputAudioTranscription", JsonObject())
                    })
                }

                val setupJson = setupMessage.toString()
                Log.d(TAG, "Sending setup message: ${setupJson.take(200)}...")
                webSocket.send(setupJson)
                trySend(WebSocketEvent.Connected("Connecting to Gemini..."))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: ${text.take(200)}...")
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}", t)
                Log.e(TAG, "Response: ${response?.code} - ${response?.message}")
                trySend(WebSocketEvent.Error(t.message ?: "Unknown error"))
                isSessionEstablished = false
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code - $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code - $reason")
                trySend(WebSocketEvent.Disconnected)
                isSessionEstablished = false
            }

            private fun handleMessage(text: String) {
                try {
                    val jsonObject = gson.fromJson(text, JsonObject::class.java)

                    // Handle setup completion
                    if (jsonObject.has("setupComplete")) {
                        isSessionEstablished = true
                        Log.d(TAG, "Session setup complete!")
                        trySend(WebSocketEvent.Connected("Connected to Gemini"))
                        return
                    }

                    // Handle server content
                    if (jsonObject.has("serverContent")) {
                        val content = jsonObject.getAsJsonObject("serverContent")

                        // Input transcription (user speech)
                        if (content.has("inputTranscription")) {
                            val transcript = content.getAsJsonObject("inputTranscription")
                            if (transcript.has("text")) {
                                val text = transcript.get("text").asString
                                Log.d(TAG, "Input transcription: $text")
                                trySend(WebSocketEvent.TranscriptReceived(text, isUser = true))
                            }
                        }

                        // Model turn (AI response)
                        if (content.has("modelTurn")) {
                            val modelTurn = content.getAsJsonObject("modelTurn")
                            if (modelTurn.has("parts")) {
                                val parts = modelTurn.getAsJsonArray("parts")
                                parts.forEach { part ->
                                    val partObj = part.asJsonObject
                                    if (partObj.has("text")) {
                                        val responseText = partObj.get("text").asString
                                        Log.d(TAG, "Model response: ${responseText.take(100)}...")

                                        // Try to parse as fraud analysis
                                        try {
                                            val jsonMatch = Regex("""\{[\s\S]*\}""").find(responseText)
                                            if (jsonMatch != null) {
                                                val analysis = gson.fromJson(jsonMatch.value, FraudAnalysis::class.java)
                                                Log.d(TAG, "Fraud analysis: is_scam=${analysis.is_scam}, confidence=${analysis.confidence_score}")

                                                // Only trigger alert if it's a scam with high confidence
                                                if (analysis.is_scam && analysis.confidence_score > 0.6f) {
                                                    Log.d(TAG, "🚨 FRAUD DETECTED!")
                                                    trySend(WebSocketEvent.FraudAnalysisReceived(analysis))
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error parsing fraud analysis: ${e.message}")
                                        }

                                        trySend(WebSocketEvent.TranscriptReceived(responseText, isUser = false))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling message: ${e.message}", e)
                }
            }
        }

        webSocket = okHttpClient.newWebSocket(request, listener)
        Log.d(TAG, "WebSocket instance created")

        awaitClose {
            Log.d(TAG, "Flow closed, disconnecting WebSocket")
            disconnect()
        }
    }

    fun sendAudioData(audioData: ByteArray) {
        if (!isSessionEstablished) {
            Log.w(TAG, "Session not established, skipping audio send")
            return
        }

        try {
            // Convert audio to base64
            val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)

            val realtimeInput = JsonObject().apply {
                add("realtimeInput", JsonObject().apply {
                    add("audio", JsonObject().apply {
                        addProperty("data", base64Audio)
                        addProperty("mimeType", "audio/pcm")
                    })
                })
            }

            webSocket?.send(realtimeInput.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio data: ${e.message}", e)
        }
    }

    fun sendTextForAnalysis(text: String) {
        if (!isSessionEstablished) {
            Log.w(TAG, "Session not established, skipping text send")
            return
        }

        try {
            val analysisMessage = JsonObject().apply {
                add("clientContent", JsonObject().apply {
                    add("turns", gson.toJsonTree(listOf(
                        mapOf("parts" to listOf(
                            mapOf("text" to "Analyze this conversation snippet for fraud: \"$text\"")
                        ))
                    )))
                    addProperty("turnComplete", true)
                })
            }

            webSocket?.send(analysisMessage.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending text for analysis: ${e.message}", e)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket...")
        try {
            webSocket?.close(1000, "User disconnected")
            webSocket = null
            isSessionEstablished = false
            Log.d(TAG, "WebSocket disconnected successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting WebSocket: ${e.message}", e)
        }
    }

    fun isConnected(): Boolean = webSocket != null && isSessionEstablished
}