package com.example.fraudlens.demo

/**
 * Lets the showcase flow pre-fill Send Money and optionally override IP/location
 * for judge demos (same behavior as manual entry + location screen).
 */
object DemoAutomationBridge {

    data class SendMoneyPrefill(
        val recipientVpa: String,
        val amount: String,
    )

    private var pendingPrefill: SendMoneyPrefill? = null
    private var pendingIpOverride: String? = null
    private var pendingLocationOverride: Pair<Double, Double>? = null

    fun queueSendMoneyPrefill(recipientVpa: String, amount: String) {
        pendingPrefill = SendMoneyPrefill(recipientVpa = recipientVpa.trim(), amount = amount.trim())
    }

    fun queueIpLocationOverrides(ip: String?, location: Pair<Double, Double>?) {
        pendingIpOverride = ip?.trim()?.takeIf { it.isNotBlank() }
        pendingLocationOverride = location
    }

    fun consumeSendMoneyPrefill(): SendMoneyPrefill? {
        val p = pendingPrefill
        pendingPrefill = null
        return p
    }

    fun consumeIpOverride(): String? {
        val v = pendingIpOverride
        pendingIpOverride = null
        return v
    }

    fun consumeLocationOverride(): Pair<Double, Double>? {
        val v = pendingLocationOverride
        pendingLocationOverride = null
        return v
    }

    fun clearAll() {
        pendingPrefill = null
        pendingIpOverride = null
        pendingLocationOverride = null
    }
}
