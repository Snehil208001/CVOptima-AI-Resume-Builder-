package com.snehil.cvoptima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "certifications")
data class LocalCertification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val issuer: String?,
    val link: String?,
    val date: String?,
    @ColumnInfo(name = "bullet_points") val bulletPoints: List<String>?,
    @ColumnInfo(name = "resume_id") val resumeId: Long
)
