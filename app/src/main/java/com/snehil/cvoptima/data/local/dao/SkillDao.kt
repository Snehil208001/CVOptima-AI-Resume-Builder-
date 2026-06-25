package com.snehil.cvoptima.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.snehil.cvoptima.data.local.entity.LocalSkill
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalSkill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalSkill>): List<Long>

    @Query("SELECT * FROM skills WHERE id = :id")
    suspend fun getById(id: Long): LocalSkill?

    @Query("SELECT * FROM skills WHERE resume_id = :resumeId")
    fun getByResumeIdFlow(resumeId: Long): Flow<List<LocalSkill>>

    @Query("SELECT * FROM skills WHERE resume_id = :resumeId")
    suspend fun getByResumeId(resumeId: Long): List<LocalSkill>

    @Update
    suspend fun update(entity: LocalSkill): Int

    @Delete
    suspend fun delete(entity: LocalSkill): Int

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM skills WHERE resume_id = :resumeId")
    suspend fun deleteByResumeId(resumeId: Long): Int
}
