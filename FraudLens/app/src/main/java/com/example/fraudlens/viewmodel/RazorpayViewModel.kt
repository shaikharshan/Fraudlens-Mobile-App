package com.example.fraudlens.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fraudlens.retrofit.RazorpayApi
import com.example.fraudlens.retrofit.RazorpayOrderRequest
import com.example.fraudlens.retrofit.RazorpayOrderResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RazorpayViewModel @Inject constructor(
    private val razorpayApi: RazorpayApi
) : ViewModel() {

    private val _orderResponse = MutableStateFlow<RazorpayOrderResponse?>(null)
    val orderResponse: StateFlow<RazorpayOrderResponse?> = _orderResponse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun createOrder(
        amount: Double,
        receipt: String,
        notes: Map<String, String>? = null,
        onSuccess: (RazorpayOrderResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Convert amount to paise (1 INR = 100 paise)
                val amountInPaise = (amount * 100).toInt()

                Log.d("RazorpayViewModel", "Creating order - Amount: $amountInPaise paise, Receipt: $receipt")

                val orderRequest = RazorpayOrderRequest(
                    amount = amountInPaise,
                    currency = "INR",
                    receipt = receipt,
                    notes = notes
                )

                val response = razorpayApi.createOrder(orderRequest) // Removed authorization parameter

                if (response.isSuccessful && response.body() != null) {
                    _orderResponse.value = response.body()
                    onSuccess(response.body()!!)
                    Log.d("RazorpayViewModel", "Order created successfully: ${response.body()}")
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = "Failed to create order: ${response.code()} - $errorBody"
                    _error.value = errorMsg
                    onError(errorMsg)
                    Log.e("RazorpayViewModel", errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Error creating order: ${e.message}"
                _error.value = errorMsg
                onError(errorMsg)
                Log.e("RazorpayViewModel", errorMsg, e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearOrder() {
        _orderResponse.value = null
    }
}