package com.snehil.cvoptima.data.remote.model

data class SummaryGenerationRequest(
    val yearsOfExp: String,
    val specialization: String,
    val targetRole: String,
    val primaryTechnologies: String
)
