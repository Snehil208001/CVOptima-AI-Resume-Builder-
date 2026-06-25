package com.snehil.cvoptima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class LocalSkill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "proficiency_level") val proficiencyLevel: String?,
    @ColumnInfo(name = "resume_id") val resumeId: Long
)
