package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class EducationDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("institution") val institution: String,
    @SerializedName("degree") val degree: String,
    @SerializedName("fieldOfStudy") val fieldOfStudy: String?,
    @SerializedName("startDate") val startDate: String?,
    @SerializedName("endDate") val endDate: String?,
    @SerializedName("gpa") val gpa: Double?
)
