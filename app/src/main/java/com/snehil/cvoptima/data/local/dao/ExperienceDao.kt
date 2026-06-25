package com.snehil.cvoptima.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.snehil.cvoptima.data.local.entity.LocalExperience
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperienceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalExperience): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalExperience>): List<Long>

    @Query("SELECT * FROM experiences WHERE id = :id")
    suspend fun getById(id: Long): LocalExperience?

    @Query("SELECT * FROM experiences WHERE resume_id = :resumeId")
    fun getByResumeIdFlow(resumeId: Long): Flow<List<LocalExperience>>

    @Query("SELECT * FROM experiences WHERE resume_id = :resumeId")
    suspend fun getByResumeId(resumeId: Long): List<LocalExperience>

    @Update
    suspend fun update(entity: LocalExperience): Int

    @Delete
    suspend fun delete(entity: LocalExperience): Int

    @Query("DELETE FROM experiences WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM experiences WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
