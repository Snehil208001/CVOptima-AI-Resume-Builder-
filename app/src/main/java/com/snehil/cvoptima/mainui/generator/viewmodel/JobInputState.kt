package com.snehil.cvoptima.mainui.generator.viewmodel

data class JobInputState(
    val companyName: String = "",
    val jobTitle: String = "",
    val jobDescription: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val taskId: String? = null
) {
    val isFormValid: Boolean 
        get() = companyName.isNotBlank() && jobTitle.isNotBlank() && jobDescription.isNotBlank()
}
