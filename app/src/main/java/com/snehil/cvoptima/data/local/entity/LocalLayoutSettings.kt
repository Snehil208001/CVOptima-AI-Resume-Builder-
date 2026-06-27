package com.snehil.cvoptima.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "layout_settings")
data class LocalLayoutSettings(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "layout_density") val layoutDensity: String,
    @ColumnInfo(name = "section_order") val sectionOrder: List<String>,
    @ColumnInfo(name = "resume_id") val resumeId: Long
)
