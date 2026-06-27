package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class LayoutConfigDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("layoutDensity") val layoutDensity: String? = null,
    @SerializedName("sectionOrder") val sectionOrder: List<String>? = null
)
