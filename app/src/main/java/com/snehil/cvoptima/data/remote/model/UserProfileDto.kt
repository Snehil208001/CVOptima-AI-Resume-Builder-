package com.snehil.cvoptima.data.remote.model

import com.google.gson.annotations.SerializedName

data class UserProfileDto(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("contactNumber") val contactNumber: String? = null,
    @SerializedName("linkedinUrl") val linkedinUrl: String? = null,
    @SerializedName("githubUrl") val githubUrl: String? = null,
    @SerializedName("portfolioUrl") val portfolioUrl: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("professionalSummary") val professionalSummary: String? = null,
    @SerializedName("experiences") val experiences: List<ExperienceDto>? = null,
    @SerializedName("educations") val educations: List<EducationDto>? = null,
    @SerializedName("skillGroups") val skillGroups: List<SkillGroupDto>? = null,
    @SerializedName("projects") val projects: List<ProjectDto>? = null,
    @SerializedName("certifications") val certifications: List<CertificationDto>? = null,
    @SerializedName("layoutConfig") val layoutConfig: LayoutConfigDto? = null
)
