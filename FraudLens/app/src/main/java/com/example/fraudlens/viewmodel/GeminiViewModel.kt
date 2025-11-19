package com.example.fraudlens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fraudlens.data.repo.GeminiRepo
import com.example.fraudlens.data.repo.ScamAnalysisResult
import com.example.fraudlens.retrofit.ScamAnalysisResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// This class represents the entire state of your screen
data class GeminiUiState(
    val isLoading: Boolean = false,
    val analysisResult: ScamAnalysisResponse? = null,
    val rawJsonBlob: String? = null, // To display the fallback JSON
    val errorMessage: String? = null
)

@HiltViewModel
class GeminiViewModel @Inject constructor(
    private val geminiRepo: GeminiRepo
) : ViewModel() {

    // Expose a single state object to the UI
    private val _uiState = MutableStateFlow(GeminiUiState())
    val uiState: StateFlow<GeminiUiState> = _uiState.asStateFlow()

    fun invoke(text: String) {
        viewModelScope.launch {
            // 1. Set loading state to true immediately
            _uiState.update { it.copy(isLoading = true, errorMessage = null, analysisResult = null, rawJsonBlob = null) }

            // 2. Call the repository
            val response = geminiRepo.invoke(text)

            // 3. Use a 'when' block to handle every possible result from the repo
            when (response) {
                is ScamAnalysisResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            analysisResult = response.analysis
                        )
                    }
                }
                is ScamAnalysisResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = response.message
                        )
                    }
                }
                is ScamAnalysisResult.JsonBlob -> {
                    // Your custom case for when JSON parsing fails
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            rawJsonBlob = response.blob
                        )
                    }
                }
                is ScamAnalysisResult.RateLimited -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "You're making too many requests. Please wait a moment."
                        )
                    }
                }
                is ScamAnalysisResult.IncompleteResponse, ScamAnalysisResult.MaxTokensReached -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "The AI returned an incomplete response. Please try again."
                        )
                    }
                }
            }
        }
    }
}