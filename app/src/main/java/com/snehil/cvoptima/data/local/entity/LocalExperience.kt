package com.snehil.cvoptima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experiences")
data class LocalExperience(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val company: String,
    val title: String,
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "end_date") val endDate: String?,
    @ColumnInfo(name = "is_current_role") val isCurrentRole: Boolean,
    val location: String?,
    val type: String?,
    @ColumnInfo(name = "bullet_points") val bulletPoints: List<String>?,
    val description: String?,
    @ColumnInfo(name = "resume_id") val resumeId: Long
)
