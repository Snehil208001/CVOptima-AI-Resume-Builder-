package com.snehil.cvoptima.data.local.dao

import androidx.room.*
import com.snehil.cvoptima.data.local.entity.LocalProject
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalProject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalProject>): List<Long>

    @Query("SELECT * FROM projects WHERE resume_id = :resumeId")
    fun getByResumeIdFlow(resumeId: Long): Flow<List<LocalProject>>

    @Query("SELECT * FROM projects WHERE resume_id = :resumeId")
    suspend fun getByResumeId(resumeId: Long): List<LocalProject>

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM projects WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
