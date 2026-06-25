package com.snehil.cvoptima.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.snehil.cvoptima.data.local.dao.EducationDao
import com.snehil.cvoptima.data.local.dao.ExperienceDao
import com.snehil.cvoptima.data.local.dao.SkillDao
import com.snehil.cvoptima.data.local.dao.TokenDao
import com.snehil.cvoptima.data.local.entity.LocalEducation
import com.snehil.cvoptima.data.local.entity.LocalExperience
import com.snehil.cvoptima.data.local.entity.LocalSkill
import com.snehil.cvoptima.data.local.entity.TokenEntity

@Database(
    entities = [
        TokenEntity::class,
        LocalExperience::class,
        LocalEducation::class,
        LocalSkill::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tokenDao(): TokenDao
    abstract fun experienceDao(): ExperienceDao
    abstract fun educationDao(): EducationDao
    abstract fun skillDao(): SkillDao
}
