package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class ExperienceOptimizationResponse(
    @SerializedName("taskId") val taskId: String?,
    @SerializedName("optimizedText") val optimizedText: String? = null
)
