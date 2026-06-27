package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class CertificationDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("title") val title: String,
    @SerializedName("issuer") val issuer: String? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("bulletPoints") val bulletPoints: List<String>? = null
)
