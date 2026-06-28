package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class AtsAnalysisRequest(
    @SerializedName("resumeText") val resumeText: String,
    @SerializedName("targetJobDescription") val targetJobDescription: String? = null
)
