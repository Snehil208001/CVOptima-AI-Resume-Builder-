package com.snehil.cvoptima.data.local

import com.snehil.cvoptima.data.local.dao.*
import com.snehil.cvoptima.data.local.entity.*

object DatabaseSeeder {
    suspend fun seedIfNeeded(
        basicInfoDao: BasicInfoDao,
        educationDao: EducationDao,
        experienceDao: ExperienceDao,
        skillGroupDao: SkillGroupDao,
        projectDao: ProjectDao,
        certificationDao: CertificationDao,
        layoutSettingsDao: LayoutSettingsDao
    ) {
        val existingInfo = basicInfoDao.getByResumeId(1L)
        if (existingInfo != null) return

        // 1. Basic Info
        basicInfoDao.insert(
            LocalBasicInfo(
                id = 1,
                name = "",
                email = "",
                contactNumber = "",
                linkedinUrl = "",
                githubUrl = "",
                portfolioUrl = "",
                location = "",
                professionalSummary = "",
                resumeId = 1L
            )
        )

        // 7. Layout settings
        layoutSettingsDao.insert(
            LocalLayoutSettings(
                id = 1,
                layoutDensity = "Normal",
                sectionOrder = listOf("Skills", "Work Experiences", "Projects", "Certifications", "Education"),
                resumeId = 1L
            )
        )
    }
}
