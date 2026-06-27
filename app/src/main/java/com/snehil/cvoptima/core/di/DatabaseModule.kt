package com.snehil.cvoptima.core.di

import android.content.Context
import androidx.room.Room
import com.snehil.cvoptima.data.local.AppDatabase
import com.snehil.cvoptima.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cvoptima_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideTokenDao(database: AppDatabase): TokenDao {
        return database.tokenDao()
    }

    @Provides
    @Singleton
    fun provideExperienceDao(database: AppDatabase): ExperienceDao {
        return database.experienceDao()
    }

    @Provides
    @Singleton
    fun provideEducationDao(database: AppDatabase): EducationDao {
        return database.educationDao()
    }

    @Provides
    @Singleton
    fun provideSkillDao(database: AppDatabase): SkillDao {
        return database.skillDao()
    }

    @Provides
    @Singleton
    fun provideBasicInfoDao(database: AppDatabase): BasicInfoDao {
        return database.basicInfoDao()
    }

    @Provides
    @Singleton
    fun provideSkillGroupDao(database: AppDatabase): SkillGroupDao {
        return database.skillGroupDao()
    }

    @Provides
    @Singleton
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    @Singleton
    fun provideCertificationDao(database: AppDatabase): CertificationDao {
        return database.certificationDao()
    }

    @Provides
    @Singleton
    fun provideLayoutSettingsDao(database: AppDatabase): LayoutSettingsDao {
        return database.layoutSettingsDao()
    }
}
