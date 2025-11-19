package com.example.fraudlens.viewmodel

import android.util.Log
import com.example.fraudlens.data.local.entities.FirestoreUser
import com.example.fraudlens.retrofit.RetrofitInstance
import com.example.fraudlens.retrofit.AbuseIPData
import com.example.fraudlens.retrofit.AbuseRiskResult
import com.example.fraudlens.retrofit.IPAbuseRiskHelper
import com.example.fraudlens.retrofit.ModelHealthOutput
import com.example.fraudlens.retrofit.ModelInput
import com.example.fraudlens.retrofit.ModelOutput
import com.example.fraudlens.ui.screens.isLocationRisky
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext

class RetrofitViewModel {

    val IPapi = RetrofitInstance.IPDB_api
    val modelAPI = RetrofitInstance.model_api


    private val _ipRiskResult = MutableStateFlow<AbuseRiskResult?>(null)
    val ipRiskResult: StateFlow<AbuseRiskResult?> = _ipRiskResult

    fun checkRisk(ip: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = IPapi.checkIP(ip)
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    data?.let {
                        val result = IPAbuseRiskHelper.evaluateRisk(it)
                        _ipRiskResult.value = result
                    }
                } else {
                    Log.e("AbuseIPDB", "Response Error: ${response.code()} ${response.message()}")
                    _ipRiskResult.value = null
                }
            } catch (e: Exception) {
                Log.e("AbuseIPDB", "Exception: ${e.message}")
                _ipRiskResult.value = null
            }
        }
    }

    suspend fun checkRiskBlocking(ip: String): AbuseRiskResult? = withContext(Dispatchers.IO) {
        try {
            val response = IPapi.checkIP(ip)
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    IPAbuseRiskHelper.evaluateRisk(it)
                }
            } else {
                Log.e("AbuseIPDB", "Error: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AbuseIPDB", "Exception: ${e.message}")
            null
        }
    }

    suspend fun getBlacklistPlain():String?{
        var blackListIP : String = ""
        return  withContext(Dispatchers.IO) {
            try{
                val response = IPapi.getBlacklistPlain()
                if(response.isSuccessful){
                    val data = response.body()
                    if(data!=null){
                        blackListIP = data.string()
                    }
                }
                if(blackListIP.isEmpty()) null else blackListIP
            }
            catch (e: Exception){
                Log.d("CustomException",e.toString())
                null
            }
        }
    }

    // AI model endpoints
    private var _modelHealth = MutableStateFlow<String>("")
    var modelHealth = _modelHealth.asStateFlow()
    suspend fun checkHealth(){

        return withContext(Dispatchers.IO) {
            try {
                val response = modelAPI.checkHealth()
                if(response.isSuccessful){
                    _modelHealth.value = response.body().toString()
                }else{
                    Log.d("modelHealth",response.message())
                }

            } catch (e: Exception){
                Log.d("CustomException",e.toString())
                e.toString()
            }
        }
    }




    private var _modelPrediction = MutableStateFlow<ModelOutput?>(null)
    var modelPrediction = _modelPrediction.asStateFlow()
    suspend fun predictFraud(input: ModelInput): ModelOutput? = withContext(Dispatchers.IO) {
        try {
            val response = modelAPI.predictFraud(input)
            if (response.isSuccessful) {
                val result = response.body()
                _modelPrediction.value = result  // update StateFlow for UI
                return@withContext result        // also return the result to caller
            }
            else{
                Log.d("modelHealth",response.message())
            }
        } catch (e: Exception) {
            Log.d("CustomException", e.toString())
        }
        return@withContext null
    }



}