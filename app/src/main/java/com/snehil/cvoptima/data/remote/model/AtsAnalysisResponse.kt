package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class AtsAnalysisResponse(
    @SerializedName("score") val score: Int,
    @SerializedName("rating") val rating: String,
    @SerializedName("layoutSuggestions") val layoutSuggestions: List<String>?,
    @SerializedName("keywordSuggestions") val keywordSuggestions: List<String>?,
    @SerializedName("metricSuggestions") val metricSuggestions: List<String>?,
    @SerializedName("verbSuggestions") val verbSuggestions: List<String>?
)
