package com.snehil.cvoptima.data.local.dao

import androidx.room.*
import com.snehil.cvoptima.data.local.entity.LocalLayoutSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface LayoutSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalLayoutSettings): Long

    @Query("SELECT * FROM layout_settings WHERE resume_id = :resumeId LIMIT 1")
    fun getByResumeIdFlow(resumeId: Long): Flow<LocalLayoutSettings?>

    @Query("SELECT * FROM layout_settings WHERE resume_id = :resumeId LIMIT 1")
    suspend fun getByResumeId(resumeId: Long): LocalLayoutSettings?

    @Query("DELETE FROM layout_settings WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
