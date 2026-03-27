package com.example.fraudlens.ui.components

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.*
import com.example.fraudlens.BuildConfig
import com.example.fraudlens.retrofit.RazorpayPaymentResult
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject

class RazorpayPaymentManager(
    private val activity: Activity
) {
    private var currentOrderId: String? = null
    private var onPaymentSuccessCallback: ((RazorpayPaymentResult) -> Unit)? = null
    private var onPaymentFailureCallback: ((RazorpayPaymentResult) -> Unit)? = null

    init {
        Checkout.preload(activity.applicationContext)

        // Set the activity as the listener if it implements PaymentResultListener
        if (activity is PaymentResultListener) {
            Log.d("RazorpayManager", "Activity implements PaymentResultListener")
        } else {
            Log.w("RazorpayManager", "Activity does NOT implement PaymentResultListener - callbacks may not work!")
        }
    }

    fun startPayment(
        orderId: String,
        amount: Double,
        name: String,
        description: String,
        userEmail: String?,
        userPhone: String?,
        onSuccess: (RazorpayPaymentResult) -> Unit,
        onFailure: (RazorpayPaymentResult) -> Unit
    ) {
        this.currentOrderId = orderId
        this.onPaymentSuccessCallback = onSuccess
        this.onPaymentFailureCallback = onFailure

        try {
            val checkout = Checkout()
            checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)

            val options = JSONObject()
            options.put("name", "FraudLens")
            options.put("description", description)
            options.put("order_id", orderId)
            options.put("currency", "INR")
            options.put("amount", (amount * 100).toInt())

            val prefill = JSONObject()
            userEmail?.let { prefill.put("email", it) }
            userPhone?.let { prefill.put("contact", it) }
            options.put("prefill", prefill)

            val theme = JSONObject()
            theme.put("color", "#3F51B5")
            options.put("theme", theme)

            Log.d("RazorpayManager", "Opening Razorpay checkout with order_id: $orderId")
            checkout.open(activity, options)
        } catch (e: Exception) {
            Log.e("RazorpayManager", "Error starting payment", e)
            onFailure(
                RazorpayPaymentResult(
                    orderId = orderId,
                    paymentId = null,
                    signature = null,
                    isSuccess = false,
                    errorMessage = "Failed to start payment: ${e.message}"
                )
            )
        }
    }

    fun handlePaymentSuccess(razorpayPaymentId: String?) {
        Log.d("RazorpayManager", "handlePaymentSuccess called - PaymentID: $razorpayPaymentId, OrderID: $currentOrderId")
        onPaymentSuccessCallback?.invoke(
            RazorpayPaymentResult(
                orderId = currentOrderId ?: "",
                paymentId = razorpayPaymentId,
                signature = null,
                isSuccess = true
            )
        )
        clearCallbacks()
    }

    fun handlePaymentError(code: Int, response: String?) {
        Log.e("RazorpayManager", "handlePaymentError called - Code: $code, Response: $response, OrderID: $currentOrderId")
        onPaymentFailureCallback?.invoke(
            RazorpayPaymentResult(
                orderId = currentOrderId ?: "",
                paymentId = null,
                signature = null,
                isSuccess = false,
                errorMessage = "Payment failed (Code $code): $response"
            )
        )
        clearCallbacks()
    }

    private fun clearCallbacks() {
        currentOrderId = null
        onPaymentSuccessCallback = null
        onPaymentFailureCallback = null
    }
}