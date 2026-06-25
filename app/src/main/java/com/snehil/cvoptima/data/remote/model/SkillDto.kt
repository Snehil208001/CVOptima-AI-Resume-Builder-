package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class SkillDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String,
    @SerializedName("proficiencyLevel") val proficiencyLevel: String?
)
