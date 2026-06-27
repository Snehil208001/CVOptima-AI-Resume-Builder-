package com.snehil.cvoptima.data.local.dao

import androidx.room.*
import com.snehil.cvoptima.data.local.entity.LocalCertification
import kotlinx.coroutines.flow.Flow

@Dao
interface CertificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalCertification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalCertification>): List<Long>

    @Query("SELECT * FROM certifications WHERE resume_id = :resumeId")
    fun getByResumeIdFlow(resumeId: Long): Flow<List<LocalCertification>>

    @Query("SELECT * FROM certifications WHERE resume_id = :resumeId")
    suspend fun getByResumeId(resumeId: Long): List<LocalCertification>

    @Query("DELETE FROM certifications WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM certifications WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
