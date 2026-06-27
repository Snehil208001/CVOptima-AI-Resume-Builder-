package com.snehil.cvoptima.data.local.dao

import androidx.room.*
import com.snehil.cvoptima.data.local.entity.LocalBasicInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface BasicInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalBasicInfo): Long

    @Query("SELECT * FROM basic_info WHERE resume_id = :resumeId LIMIT 1")
    fun getByResumeIdFlow(resumeId: Long): Flow<LocalBasicInfo?>

    @Query("SELECT * FROM basic_info WHERE resume_id = :resumeId LIMIT 1")
    suspend fun getByResumeId(resumeId: Long): LocalBasicInfo?

    @Query("DELETE FROM basic_info WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
