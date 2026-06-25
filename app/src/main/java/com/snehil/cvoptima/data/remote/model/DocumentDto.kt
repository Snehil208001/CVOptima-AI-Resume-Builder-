package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class DocumentDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("jobTitle") val jobTitle: String? = null,
    @SerializedName("companyName") val companyName: String? = null,
    @SerializedName("jdText") val jdText: String? = null,
    @SerializedName("resumeMd") val resumeMd: String? = null,
    @SerializedName("status") val status: String? = null
)
