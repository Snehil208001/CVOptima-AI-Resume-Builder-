package com.snehil.cvoptima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "educations")
data class LocalEducation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val institution: String,
    val degree: String,
    @ColumnInfo(name = "field_of_study") val fieldOfStudy: String?,
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "end_date") val endDate: String?,
    val gpa: Double?,
    val score: String?,
    val location: String?,
    @ColumnInfo(name = "resume_id") val resumeId: Long
)
