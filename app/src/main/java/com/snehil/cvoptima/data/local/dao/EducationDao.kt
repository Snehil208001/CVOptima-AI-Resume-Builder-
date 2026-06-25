package com.snehil.cvoptima.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.snehil.cvoptima.data.local.entity.LocalEducation
import kotlinx.coroutines.flow.Flow

@Dao
interface EducationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalEducation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalEducation>): List<Long>

    @Query("SELECT * FROM educations WHERE id = :id")
    suspend fun getById(id: Long): LocalEducation?

    @Query("SELECT * FROM educations WHERE resume_id = :resumeId")
    fun getByResumeIdFlow(resumeId: Long): Flow<List<LocalEducation>>

    @Query("SELECT * FROM educations WHERE resume_id = :resumeId")
    suspend fun getByResumeId(resumeId: Long): List<LocalEducation>

    @Update
    suspend fun update(entity: LocalEducation): Int

    @Delete
    suspend fun delete(entity: LocalEducation): Int

    @Query("DELETE FROM educations WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM educations WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
