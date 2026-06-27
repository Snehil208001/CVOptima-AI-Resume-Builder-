package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class ProjectDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("title") val title: String,
    @SerializedName("link") val link: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("techStack") val techStack: String? = null,
    @SerializedName("bulletPoints") val bulletPoints: List<String>? = null
)
