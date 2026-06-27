package com.snehil.cvoptima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "basic_info")
data class LocalBasicInfo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val email: String?,
    @ColumnInfo(name = "contact_number") val contactNumber: String?,
    @ColumnInfo(name = "linkedin_url") val linkedinUrl: String?,
    @ColumnInfo(name = "github_url") val githubUrl: String?,
    @ColumnInfo(name = "portfolio_url") val portfolioUrl: String?,
    val location: String?,
    @ColumnInfo(name = "professional_summary") val professionalSummary: String?,
    @ColumnInfo(name = "resume_id") val resumeId: Long
)
