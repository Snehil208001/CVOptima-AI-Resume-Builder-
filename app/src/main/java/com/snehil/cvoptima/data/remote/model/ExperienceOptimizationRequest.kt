package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class ExperienceOptimizationRequest(
    @SerializedName("rawExperience") val rawExperience: String,
    @SerializedName("targetJobDescription") val targetJobDescription: String
)
