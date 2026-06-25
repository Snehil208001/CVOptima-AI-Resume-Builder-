package com.snehil.cvoptima.core.di

import android.content.Context
import androidx.room.Room
import com.snehil.cvoptima.data.local.AppDatabase
import com.snehil.cvoptima.data.local.dao.EducationDao
import com.snehil.cvoptima.data.local.dao.ExperienceDao
import com.snehil.cvoptima.data.local.dao.SkillDao
import com.snehil.cvoptima.data.local.dao.TokenDao
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
}
