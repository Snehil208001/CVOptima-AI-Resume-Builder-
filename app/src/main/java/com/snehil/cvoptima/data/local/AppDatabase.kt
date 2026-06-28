package com.snehil.cvoptima.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Transaction
import com.snehil.cvoptima.data.local.dao.*
import com.snehil.cvoptima.data.local.entity.*

@Database(
    entities = [
        TokenEntity::class,
        LocalExperience::class,
        LocalEducation::class,
        LocalSkill::class,
        LocalBasicInfo::class,
        LocalSkillGroup::class,
        LocalProject::class,
        LocalCertification::class,
        LocalLayoutSettings::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun experienceDao(): ExperienceDao
    abstract fun educationDao(): EducationDao
    abstract fun skillDao(): SkillDao
    abstract fun basicInfoDao(): BasicInfoDao
    abstract fun skillGroupDao(): SkillGroupDao
    abstract fun projectDao(): ProjectDao
    abstract fun certificationDao(): CertificationDao
    abstract fun layoutSettingsDao(): LayoutSettingsDao

    @Transaction
    suspend fun clearAllUserData() {
        // 1. Clear tables containing dependent child items first
        educationDao().deleteByResumeId(1L)
        experienceDao().deleteByResumeId(1L)
        projectDao().deleteByResumeId(1L)
        certificationDao().deleteByResumeId(1L)
        skillGroupDao().deleteByResumeId(1L)
        
        // 2. Clear parent profile and structural meta-data last
        basicInfoDao().deleteByResumeId(1L)
        layoutSettingsDao().deleteByResumeId(1L)
    }
}
