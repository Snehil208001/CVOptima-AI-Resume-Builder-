package com.snehil.cvoptima.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
}
