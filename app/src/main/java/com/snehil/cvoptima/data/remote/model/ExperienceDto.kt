package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class ExperienceDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("company") val company: String,
    @SerializedName("title") val title: String,
    @SerializedName("startDate") val startDate: String?,
    @SerializedName("endDate") val endDate: String?,
    @SerializedName("isCurrentRole") val isCurrentRole: Boolean,
    @SerializedName("description") val description: String?,
    @SerializedName("location") val location: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("bulletPoints") val bulletPoints: List<String>? = null
)
