package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class SkillGroupDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("label") val label: String,
    @SerializedName("skills") val skills: List<String>? = null
)
