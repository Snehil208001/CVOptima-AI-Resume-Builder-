package com.snehil.cvoptima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skill_groups")
data class LocalSkillGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val skills: List<String>,
    @ColumnInfo(name = "resume_id") val resumeId: Long
)
