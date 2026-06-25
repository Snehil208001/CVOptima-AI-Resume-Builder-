package com.snehil.cvoptima.mainui.homescreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.domain.model.Document
import com.snehil.cvoptima.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val documents: List<Document>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            documentRepository.getRecentDocuments()
                .catch { exception ->
                    _uiState.value = HomeUiState.Error(exception.message ?: "Failed to load documents")
                }
                .collect { documents ->
                    _uiState.value = HomeUiState.Success(documents)
                }
        }
    }

    fun tailorNewResume(jobTitle: String, companyName: String, jdText: String) {
        viewModelScope.launch {
            documentRepository.createDocument(
                jobTitle = jobTitle,
                companyName = companyName,
                jdText = jdText,
                resumeMd = "# $jobTitle at $companyName\n\nOptimized Resume content matching job description:\n$jdText"
            )
            loadDocuments()
        }
    }
}
