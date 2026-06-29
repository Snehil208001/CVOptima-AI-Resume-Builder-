package com.snehil.cvoptima.mainui.generator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snehil.cvoptima.data.local.dao.ExperienceDao
import com.snehil.cvoptima.data.remote.ApiService
import com.snehil.cvoptima.data.remote.model.ExperienceOptimizationRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JobInputViewModel @Inject constructor(
    private val experienceDao: ExperienceDao,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobInputState())
    val uiState: StateFlow<JobInputState> = _uiState.asStateFlow()

    fun onCompanyNameChanged(companyName: String) {
        _uiState.update { it.copy(companyName = companyName) }
    }

    fun onJobTitleChanged(jobTitle: String) {
        _uiState.update { it.copy(jobTitle = jobTitle) }
    }

    fun onJobDescriptionChanged(jobDescription: String) {
        _uiState.update { it.copy(jobDescription = jobDescription) }
    }

    fun optimizeExperience() {
        val currentState = _uiState.value
        if (!currentState.isFormValid) {
            _uiState.update { it.copy(errorMessage = "All fields are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch local work experiences
                val experiences = experienceDao.getByResumeId(1L)
                if (experiences.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Please add at least one work experience to your Profile first."
                        )
                    }
                    return@launch
                }

                val rawExperience = experiences.joinToString(separator = "\n\n") { exp ->
                    "Company: ${exp.company}\nTitle: ${exp.title}\nRole Dates: ${exp.startDate ?: "N/A"} to ${exp.endDate ?: "N/A"}\nDescription: ${exp.description ?: "N/A"}"
                }

                // Call endpoint
                val request = ExperienceOptimizationRequest(
                    rawExperience = rawExperience,
                    targetJobDescription = "Company: ${currentState.companyName}\nDesired Title: ${currentState.jobTitle}\nJob Description:\n${currentState.jobDescription}"
                )
                val response = apiService.optimizeExperience(request)
                
                val taskId = response.taskId
                if (!taskId.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoading = false, taskId = taskId) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Backend did not return a valid task ID") }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        errorMessage = e.message ?: "An unexpected error occurred during optimization"
                    ) 
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = JobInputState()
    }
}
