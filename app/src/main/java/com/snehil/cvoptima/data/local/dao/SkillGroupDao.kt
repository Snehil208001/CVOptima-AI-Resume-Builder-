package com.snehil.cvoptima.data.local.dao

import androidx.room.*
import com.snehil.cvoptima.data.local.entity.LocalSkillGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalSkillGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalSkillGroup>): List<Long>

    @Query("SELECT * FROM skill_groups WHERE resume_id = :resumeId")
    fun getByResumeIdFlow(resumeId: Long): Flow<List<LocalSkillGroup>>

    @Query("SELECT * FROM skill_groups WHERE resume_id = :resumeId")
    suspend fun getByResumeId(resumeId: Long): List<LocalSkillGroup>

    @Query("DELETE FROM skill_groups WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
