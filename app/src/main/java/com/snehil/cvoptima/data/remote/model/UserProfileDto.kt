package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class UserProfileDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("experiences") val experiences: List<ExperienceDto>? = null,
    @SerializedName("educations") val educations: List<EducationDto>? = null,
    @SerializedName("skills") val skills: List<SkillDto>? = null
)
